package com.gitggal.clothesplz.service.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.dto.clothes.ClothesDto;
import com.gitggal.clothesplz.entity.clothes.Clothes;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeRepository;
import com.gitggal.clothesplz.service.ai.ClothesAi;
import com.gitggal.clothesplz.service.ai.HtmlProductExtractor;
import com.gitggal.clothesplz.service.ai.HtmlProductExtractor.ScrapeResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OpenAiClothesAi implements ClothesAi {

  private static final String UNKNOWN_NAME = "알 수 없는 의상";

  private final ChatClient chatClient;
  private final ObjectMapper objectMapper;
  private final ClothesAttributeRepository clothesAttributeRepository;
  private final HtmlProductExtractor htmlExtractor;

  public OpenAiClothesAi(
      ChatClient.Builder chatClientBuilder,
      ObjectMapper objectMapper,
      ClothesAttributeRepository clothesAttributeRepository,
      HtmlProductExtractor htmlExtractor
  ) {
    this.chatClient = chatClientBuilder.build();
    this.objectMapper = objectMapper;
    this.clothesAttributeRepository = clothesAttributeRepository;
    this.htmlExtractor = htmlExtractor;
  }

  // ── 의상 추천 ──────────────────────────────────────────────────────────────

  @Override
  public List<UUID> recommendClothesIds(
      Weather weather,
      List<Clothes> allClothes,
      short tempSensitivity
  ) {
    try {
      log.info("[OpenAI] 의상 추천 시작");
      String content = chatClient.prompt()
          .system(recommendSystemPrompt())
          .user(buildRecommendPrompt(weather, allClothes, tempSensitivity))
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
        당신은 패션 어드바이저입니다. 날씨 정보와 옷 정보를 보고 가장 적합한 옷을 추천해주세요.
        추천은 실용성을 유지하면서도 가능한 범위에서 조합이 단조롭지 않게 다양성을 확보하세요.
        같은 타입만 반복 선택하지 말고, 날씨 조건 + 사용자의 온도 민감도를 고려해서 타입/속성을 분산해 고르세요.
        추천 목록을 착용하고 야외 활동을 할 수있게 상의/하의/신발/악세사리 필수 구성으로 하세요.
        이전 요청과 같은 내용을 추천하지 않도록, 중복되지 않는 후보군 3종을 만들고 그 중에서 랜덤하게 추천해주세요.
        같은 타입의 옷은 2개까지만 추천하세요. 예를 들어 상의 3개, 하의 3개 등 금지
        만약 필수구성 항목이 없다면 생략하세요.
        반드시 JSON 형식으로만 응답하세요: {"recommendedIds": ["uuid1", "uuid2", ...]}
        코드 블럭 표시는 반드시 제거하고 내용만 주세요.
        코드 블럭 예시는 아래와 같습니다
        ```json
        ```
        추천 ID는 반드시 제공된 목록에 있는 것만 사용하고, 5개 추천하세요.
        """;
  }

  private String buildRecommendPrompt(
      Weather weather,
      List<Clothes> allClothes,
      short tempSensitivity
  ) {
    // key: 의상ID, value: 의상 속성 목록
    Map<UUID, List<String>> attributesByClothesId = buildAttributesByClothesId(allClothes);
    return buildWeatherSection(weather)
           + buildSensitivitySection(tempSensitivity)
           + buildClothesSection(allClothes, attributesByClothesId);
  }

  private String buildSensitivitySection(short tempSensitivity) {
    return """
        사용자 온도 민감도:
        - tempSensitivity: %d (1에 가까울수록 추위를 탐, 5에 가까울수록 더위를 탐)
        
        """.formatted(tempSensitivity);
  }

  private String buildWeatherSection(Weather weather) {
    return """
        현재 날씨:
        - 현재 기온: %.1f°C (최저: %.1f°C, 최고: %.1f°C)
        - 하늘 상태: %s, 강수 유형: %s (강수 확률: %.0f%%)
        - 바람: %s, 습도: %.0f%%
        
        보유 옷 목록:
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
      sb.append("""
          - {"id":"%s","name":"%s","type":"%s","attributes":[%s]}
          """.formatted(
          clothes.getId(),
          clothes.getName(),
          clothes.getType(),
          attributes.stream().map(a -> "\"" + a + "\"").collect(Collectors.joining(", "))
      ));
    }
    sb.append("\n위 날씨에 어울리는 옷의 ID를 추천해주세요.");
    sb.append("\n단, 조건을 만족하는 후보가 여러 개면 속성과 타입이 한쪽으로 쏠리지 않게 분산해서 선택해주세요.");
    return sb.toString();
  }

  private Map<UUID, List<String>> buildAttributesByClothesId(List<Clothes> allClothes) {
    if (allClothes.isEmpty()) {
      return Map.of();
    }
    List<UUID> clothesIds = allClothes.stream().map(Clothes::getId).toList();

    return clothesAttributeRepository.findAllByClothesIdIn(clothesIds).stream()
        .collect(Collectors.groupingBy(
            attr -> attr.getClothes().getId(),
            Collectors.mapping(
                attr -> attr.getDefinition().getName() + ":" + attr.getValue(),
                Collectors.toList()
            )
        ));
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
      String name = node.path("name").asText("");
      ClothesType type = parseType(node.path("type").asText(""));

      return new AiClassified(name.isBlank() ? UNKNOWN_NAME : name, type);
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
