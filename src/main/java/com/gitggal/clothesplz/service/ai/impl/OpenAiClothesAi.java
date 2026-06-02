package com.gitggal.clothesplz.service.ai.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.dto.clothes.ClothesDto;
import com.gitggal.clothesplz.entity.clothes.Clothes;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.service.ai.ClothesAi;
import com.gitggal.clothesplz.service.ai.HtmlProductExtractor;
import com.gitggal.clothesplz.service.ai.HtmlProductExtractor.ScrapeResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OpenAiClothesAi implements ClothesAi {

  private static final String UNKNOWN_NAME = "알 수 없는 의상";
  private static final int MAX_ATTRIBUTES_PER_CLOTHES = 5;
  private static final int MIN_RECOMMENDATIONS = 4;
  private static final int MAX_RECOMMENDATIONS = 7;

  private final ChatClient chatClient;
  private final ObjectMapper objectMapper;
  private final HtmlProductExtractor htmlExtractor;

  public OpenAiClothesAi(
      ChatClient.Builder chatClientBuilder,
      ObjectMapper objectMapper,
      HtmlProductExtractor htmlExtractor
  ) {
    this.chatClient = chatClientBuilder.build();
    this.objectMapper = objectMapper;
    this.htmlExtractor = htmlExtractor;
  }

  // ── 의상 추천 ──────────────────────────────────────────────────────────────

  @Override
  public List<UUID> recommendClothesIds(
      Weather weather,
      List<Clothes> allClothes,
      short tempSensitivity,
      Map<UUID, List<String>> attributesByClothesId
  ) {
    String userPrompt = buildRecommendPrompt(
        weather,
        allClothes,
        tempSensitivity,
        attributesByClothesId
    );
    try {
      log.info("[OpenAI] 의상 추천 시작");
      String content = chatClient.prompt()
          .system(recommendSystemPrompt())
          .user(userPrompt)
          .call()
          .content();
      log.info("[OpenAI] 의상 추천 완료");
      return parseIds(content);
    } catch (Exception e) {
      log.warn("[OpenAI] 의상 추천 실패", e);
      return List.of();
    }
  }

  private String recommendSystemPrompt() {
    return """
        날씨, 온도 민감도, 의상 목록을 보고 어울리는 의상 ID를 %d~%d개 고르세요.
        규칙:
        - 제공된 id 중에서만 선택
        - 날씨와 tempSensitivity에 맞는 의상 우선
        - 적합한 후보 안에서 매번 다양한 조합이 나오도록 선택
        - 타입이 한쪽으로 치우치지 않게 분산
        - 동일 타입은 최대 2개
        - 적절한 항목이 부족하면 가능한 만큼만 추천
        출력:
        {"recommendedIds":["uuid1","uuid2", ...]}
        JSON 외 텍스트/코드블록 금지
        """.formatted(MIN_RECOMMENDATIONS, MAX_RECOMMENDATIONS);
  }

  private String buildRecommendPrompt(
      Weather weather,
      List<Clothes> allClothes,
      short tempSensitivity,
      Map<UUID, List<String>> attributesByClothesId
  ) {
    return buildWeatherSection(weather)
           + buildSensitivitySection(tempSensitivity)
           + buildClothesSection(allClothes, attributesByClothesId);
  }

  private String buildSensitivitySection(short tempSensitivity) {
    return """
        민감도:
        - tempSensitivity: %d (1=추위 민감, 5=더위 민감)
        
        """.formatted(tempSensitivity);
  }

  private String buildWeatherSection(Weather weather) {
    return """
        날씨:
        - temp: %.1f°C (min: %.1f°C, max: %.1f°C)
        - sky: %s, precip: %s, pop: %.0f%%
        - wind: %s, humidity: %.0f%%
        
        clothes:
        """.formatted(
        weather.getTemperatureCurrent(),
        weather.getTemperatureMin(),
        weather.getTemperatureMax(),
        weather.getSkyStatus(),
        weather.getPrecipitationType(),
        weather.getPrecipitationProbability(),
        weather.getWindPhrase(),
        weather.getHumidity());
  }

  private String buildClothesSection(
      List<Clothes> allClothes,
      Map<UUID, List<String>> attributesByClothesId
  ) {
    StringBuilder sb = new StringBuilder();

    for (Clothes clothes : allClothes) {
      List<String> attributes = attributesByClothesId.getOrDefault(clothes.getId(), List.of());
      List<String> attrs = attributes.stream()
          .limit(MAX_ATTRIBUTES_PER_CLOTHES)
          .toList();
      String serializedAttrs;
      try {
        serializedAttrs = objectMapper.writeValueAsString(attrs);
      } catch (JsonProcessingException e) {
        log.warn("[OpenAI] attrs 직렬화 실패. clothesId={}", clothes.getId(), e);
        serializedAttrs = "[]";
      }
      sb.append("""
          - {"id":"%s","type":"%s","attrs":%s}
          """.formatted(
          clothes.getId(),
          clothes.getType(),
          serializedAttrs
      ));
    }
    return sb.toString();
  }

  // AI 응답을 파싱 -> 의상 ID만 추출
  private List<UUID> parseIds(String content) throws Exception {
    if (content == null || content.isBlank()) {
      return List.of();
    }
    JsonNode node = objectMapper.readTree(content);
    List<UUID> ids = new ArrayList<>();
    for (JsonNode item : node.path("recommendedIds")) {
      try {
        ids.add(UUID.fromString(item.asText()));
      } catch (IllegalArgumentException e) {
        log.warn("[OpenAI] 잘못된 UUID 스킵: {}", item.asText());
      }
    }
    return ids;
  }

  // ── URL 기반 의상 추출 ──────────────────────────────────────────────────────

  @Override
  public ClothesDto extractClothesByUrl(String url) {
    try {
      log.info("[OpenAI] URL 기반 의상 정보 추출 시작");
      ScrapeResult scrape = htmlExtractor.scrape(url);

      AiClassified classified = classifyWithAi(scrape.nameCandidates(), url);
      log.info("[OpenAI] URL 기반 의상 정보 추출 완료");

      return new ClothesDto(
          null,
          null,
          classified.name(),
          scrape.imageUrl(),
          classified.type(),
          List.of()
      );
    } catch (Exception e) {
      log.warn("[OpenAI] URL 기반 의상 정보 추출 실패", e);
      return new ClothesDto(null, null, UNKNOWN_NAME, null, ClothesType.ETC, List.of());
    }
  }

  // 내부용 record
  private record AiClassified(
      String name,
      ClothesType type
  ) {

  }

  private AiClassified classifyWithAi(List<String> nameCandidates, String url) {
    if (nameCandidates.isEmpty()) {
      return new AiClassified(UNKNOWN_NAME, ClothesType.ETC);
    }
    try {
      String systemPrompt = """
          당신은 의상 정보 분류기입니다.
          상품명 후보들과 URL을 보고 가장 적절한 상품명과 의상 타입을 결정하세요.
          반드시 JSON 형식으로만 응답하세요: {"name":"상품명","type":"TOP|BOTTOM|DRESS|OUTER|UNDERWEAR|ACCESSORY|SHOES|SOCKS|HAT|BAG|SCARF|ETC"}
          코드 블럭 표시는 반드시 제거하고 내용만 주세요.
          코드 블럭 예시는 아래와 같습니다
          ```json
          ```
          확신이 낮은 타입은 ETC를 반환하세요.
          """;
      String userPrompt = "상품명 후보: %s\nURL: %s".formatted(nameCandidates, url);

      String content = chatClient.prompt()
          .system(systemPrompt)
          .user(userPrompt)
          .call()
          .content();

      // 의상 정보 못찾으면 기타 아이템
      if (content == null || content.isBlank()) {
        log.info("[OpenAI] 의상 정보 추출에 실패하여 기타 설정");
        return new AiClassified(nameCandidates.get(0), ClothesType.ETC);
      }

      JsonNode node = objectMapper.readTree(content);
      String name = node.path("name").asText("").trim();
      ClothesType type = parseType(node.path("type").asText(""));

      String resolvedName = name.isBlank() ? nameCandidates.get(0) : name;
      return new AiClassified(resolvedName, type);
    } catch (Exception e) {
      log.warn("[OpenAI] 의상 분류 실패", e);
      return new AiClassified(nameCandidates.get(0), ClothesType.ETC);
    }
  }

  private ClothesType parseType(String rawType) {
    if (rawType == null || rawType.isBlank()) {
      return ClothesType.ETC;
    }
    try {
      return ClothesType.valueOf(rawType.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return ClothesType.ETC;
    }
  }
}
