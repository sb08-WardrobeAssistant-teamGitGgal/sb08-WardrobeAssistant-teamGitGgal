package com.gitggal.clothesplz.service.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.dto.clothes.ClothesDto;
import com.gitggal.clothesplz.dto.clothes.ExtractedProduct;
import com.gitggal.clothesplz.entity.clothes.Clothes;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeRepository;
import com.gitggal.clothesplz.service.ai.ClothesAi;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OpenAiClothesAi implements ClothesAi {

  private final ChatClient chatClient;
  private final ObjectMapper objectMapper;
  private final ClothesAttributeRepository clothesAttributeRepository;

  private static final String UNKNOWN_NAME = "알 수 없는 의상";

  public OpenAiClothesAi(
      ChatClient.Builder chatClientBuilder,
      ObjectMapper objectMapper,
      ClothesAttributeRepository clothesAttributeRepository
  ) {
    this.chatClient = chatClientBuilder.build();
    this.objectMapper = objectMapper;
    this.clothesAttributeRepository = clothesAttributeRepository;
  }

  @Override
  public List<UUID> recommendClothesIds(Weather weather, List<Clothes> allClothes) {
    try {
      String systemPrompt = """
          당신은 패션 어드바이저입니다. 날씨 정보와 옷 목록을 보고 가장 적합한 옷을 추천해주세요.
          추천은 실용성을 유지하면서도 가능한 범위에서 조합이 단조롭지 않게 다양성을 확보하세요.
          같은 타입만 반복 선택하지 말고, 날씨 조건을 해치지 않는 선에서 타입/속성을 분산해 고르세요.
          반드시 JSON 형식으로만 응답하세요: {"recommendedIds": ["uuid1", "uuid2", ...]}
          추천 ID는 반드시 제공된 목록에 있는 것만 사용하고, 최대 10개까지만 추천하세요.
          """;
      String userPrompt = buildPrompt(weather, allClothes);

      log.info("[OpenAI] 의상 추천 시작");

      String content = chatClient.prompt()
          .system(systemPrompt)
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

  private String buildPrompt(Weather weather, List<Clothes> allClothes) {
    Map<UUID, List<String>> attributesByClothesId = buildAttributesByClothesId(allClothes);

    StringBuilder prompt = new StringBuilder("""
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
        weather.getHumidity()));

    for (Clothes clothes : allClothes) {
      List<String> attributes = attributesByClothesId.getOrDefault(clothes.getId(), List.of());

      prompt.append(
          """
              - {
                "id":"%s",
                "name":"%s",
                "type":"%s",
                "attributes":[%s]
              }
              """.formatted(
              clothes.getId(),
              clothes.getName(),
              clothes.getType(),
              attributes.stream()
                  .map(attr -> "\"" + attr + "\"")
                  .collect(Collectors.joining(", "))
          ));
    }

    prompt.append("\n위 날씨에 어울리는 옷의 ID를 추천해주세요.");
    prompt.append("\n단, 조건을 만족하는 후보가 여러 개면 속성과 타입이 한쪽으로 쏠리지 않게 분산해서 선택해주세요.");

    return prompt.toString();
  }

  private Map<UUID, List<String>> buildAttributesByClothesId(List<Clothes> allClothes) {
    if (allClothes.isEmpty()) {
      return Map.of();
    }

    List<UUID> clothesIds = allClothes.stream()
        .map(Clothes::getId)
        .toList();

    return clothesAttributeRepository.findAllByClothesIdIn(clothesIds).stream()
        .collect(
            Collectors.groupingBy(attr -> attr.getClothes().getId(),
                Collectors.mapping(attr ->
                    attr.getDefinition().getName() + ":" + attr.getValue(), Collectors.toList()
                )
            ));
  }

  private List<UUID> parseIds(String content) throws Exception {
    if (content == null || content.isBlank()) {
      return List.of();
    }

    JsonNode contentNode = objectMapper.readTree(content);
    List<UUID> ids = new ArrayList<>();

    for (JsonNode node : contentNode.path("recommendedIds")) {
      try {
        ids.add(UUID.fromString(node.asText()));
      } catch (IllegalArgumentException e) {
        log.warn("[OpenAi] 의상 목록의 잘못된 UUID 스킵: {}", node.asText());
      }
    }

    return ids;
  }

  @Override
  public ClothesDto extractClothesByUrl(String url) {
    try {
      log.info("[OpenAI] URL 기반 의상 정보 추출 시작");

      ExtractedProduct extracted = extractProductFromHtml(url);
      ClothesType type = inferType(extracted.name(), url);

      log.info("[OpenAI] URL 기반 의상 정보 추출 완료");
      return new ClothesDto(null, null, extracted.name(), extracted.imageUrl(), type, List.of());
    } catch (Exception e) {
      log.warn("[OpenAI] URL 기반 의상 정보 추출 실패", e);
      return new ClothesDto(null, null, UNKNOWN_NAME, null, ClothesType.ETC, List.of());
    }
  }

  /**
   * 페이지의 상품명, 이미지 URL을 스크롤링.
   *
   * @param url 접속 URL 정보
   * @return 상품명, 대표 이미지 URL
   */
  private ExtractedProduct extractProductFromHtml(String url) {
    Document doc;
    try {
      doc = Jsoup.connect(url)
          .userAgent("Mozilla/5.0") // 브라우저 요청처럼 보이도록 User-Agent 헤더 추가
          .timeout(5000)            // 5초 안에 응답을 못 받으면 실패 처리
          .get();                   // GET 요청 실행 후 HTML을 Document로 파싱
    } catch (IOException e) {
      log.warn("[OpenAI] 상품 HTML 조회 실패. url={}", url, e);
      return ExtractedProduct.of(UNKNOWN_NAME, null);
    }

    // 상품명 후보를 우선순위대로 검사해서 처음으로 비어있지 않은 값을 사용
    String name = firstText(
        doc.select("meta[property=og:title]").attr("content"),
        doc.select("meta[name=twitter:title]").attr("content"),
        doc.select("[class*=GoodsName__]").text(),
        doc.select("h1").text()
    );

    // 이미지 URL 후보를 우선순위대로 검사해서 처음으로 비어있지 않은 값을 사용
    String imageUrl = firstText(
        doc.select("meta[property=og:image]").attr("content"),
        doc.select("meta[name=twitter:image]").attr("content"),
        firstImageUrl(doc)
    );
    imageUrl = validateImageUrl(imageUrl);

    return ExtractedProduct.of(name == null ? UNKNOWN_NAME : name, imageUrl);
  }

  // 타입 추론
  private ClothesType inferType(String name, String url) {
    if (name == null || name.isBlank() || UNKNOWN_NAME.equals(name)) {
      return ClothesType.ETC;
    }

    try {
      String systemPrompt = """
          당신은 의상 타입 분류기입니다.
          반드시 JSON 형식으로만 응답하세요: {"type":"TOP|BOTTOM|DRESS|OUTER|UNDERWEAR|ACCESSORY|SHOES|SOCKS|HAT|BAG|SCARF|ETC"}
          상품명/URL만 보고 가장 가능성 높은 타입 하나를 고르세요.
          확신이 낮으면 ETC를 반환하세요.
          """;
      String userPrompt = """
          상품명: %s
          URL: %s
          """.formatted(name, url);

      String content = chatClient.prompt()
          .system(systemPrompt)
          .user(userPrompt)
          .call()
          .content();

      if (content == null || content.isBlank()) {
        return ClothesType.ETC;
      }
      JsonNode node = objectMapper.readTree(content);
      return parseType(node.path("type").asText(""));
    } catch (Exception e) {
      log.warn("[OpenAI] type 분류 실패", e);
      return ClothesType.ETC;
    }
  }

  private String firstText(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) {
        return value.trim();
      }
    }
    return null;
  }

  private String firstImageUrl(Document doc) {
    Element image = doc.selectFirst("img");
    return image == null ? null : image.absUrl("src");
  }

  private String validateImageUrl(String imageUrl) {
    if (imageUrl == null || imageUrl.isBlank()) {
      return null;
    }
    if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
      return null;
    }
    return imageUrl;
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
