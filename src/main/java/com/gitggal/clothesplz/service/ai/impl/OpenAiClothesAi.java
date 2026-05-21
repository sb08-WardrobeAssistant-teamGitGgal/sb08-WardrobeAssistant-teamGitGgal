package com.gitggal.clothesplz.service.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.dto.clothes.ClothesDto;
import com.gitggal.clothesplz.entity.clothes.Clothes;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeRepository;
import com.gitggal.clothesplz.service.ai.ClothesAi;
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

  private final ChatClient chatClient;
  private final ObjectMapper objectMapper;
  private final ClothesAttributeRepository clothesAttributeRepository;

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
    // TODO: [심화] 구매 링크로 의상 조회 기능 작업
    throw new UnsupportedOperationException("extractClothesByUrl is not implemented yet");
  }
}
