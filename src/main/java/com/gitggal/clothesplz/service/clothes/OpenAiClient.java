package com.gitggal.clothesplz.service.clothes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gitggal.clothesplz.entity.clothes.Clothes;
import com.gitggal.clothesplz.entity.weather.Weather;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Component
@Slf4j
public class OpenAiClient {

  private static final String BASE_URL = "https://api.openai.com/v1/chat/completions";
  private static final String MODEL = "gpt-4o-mini";

  private final WebClient webClient;
  private final ObjectMapper objectMapper;

  public OpenAiClient(@Value("${openai.api.key}") String apiKey, ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    HttpClient httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
        .responseTimeout(Duration.ofSeconds(30)).doOnConnected(
            conn -> conn.addHandlerLast(new ReadTimeoutHandler(30)).addHandlerLast(new WriteTimeoutHandler(30)));

    this.webClient = WebClient.builder().baseUrl(BASE_URL).defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .clientConnector(new ReactorClientHttpConnector(httpClient)).build();
  }

  /**
   * 날씨와 옷 목록을 OpenAI에 전달해 추천 옷 ID 목록을 반환한다.
   * 호출 실패 시 빈 리스트를 반환 (호출부에서 fallback 처리).
   */
  public List<UUID> recommendClothesIds(Weather weather, List<Clothes> allClothes) {
    try {
      ObjectNode body = buildRequestBody(weather, allClothes);

      log.info("[OpenAiClient] OpenAI 호출 시작 (옷 {}개)", allClothes.size());

      String raw = webClient.post().bodyValue(body).retrieve().bodyToMono(String.class).block();

      List<UUID> result = parseIds(raw);
      log.info("[OpenAiClient] OpenAI 호출 성공 - 추천 ID {}개: {}", result.size(), result);
      return result;
    } catch (Exception e) {
      log.warn("[OpenAiClient] 호출 실패, fallback 사용: {}", e.getMessage());
      return List.of();
    }
  }

  private ObjectNode buildRequestBody(Weather weather, List<Clothes> allClothes) {
    ObjectNode body = objectMapper.createObjectNode();
    body.put("model", MODEL);

    ArrayNode messages = body.putArray("messages");

    // 행동 지침
    ObjectNode system = messages.addObject();
    system.put("role", "system");
    system.put("content", """
            당신은 패션 어드바이저입니다. 날씨 정보와 옷 목록을 보고 가장 적합한 옷을 추천해주세요.
            반드시 JSON 형식으로만 응답하세요: {"recommendedIds": ["uuid1", "uuid2", ...]}
            추천 ID는 반드시 제공된 목록에 있는 것만 사용하고, 최대 10개까지만 추천하세요.
        """);

    // 실제 프롬프트
    ObjectNode user = messages.addObject();
    user.put("role", "user");
    user.put("content", buildPrompt(weather, allClothes));

    // 응답 포멧
    body.putObject("response_format").put("type", "json_object");
    return body;
  }

  private String buildPrompt(Weather weather, List<Clothes> allClothes) {
    StringBuilder sb = new StringBuilder();
    sb.append("""
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
        weather.getHumidity())
    );

    for (Clothes c : allClothes) {
      sb.append("""
          - {
            "id":"%s",
            "name":"%s",
            "type":"%s"
          }
          """.formatted(
          c.getId(),
          c.getName(),
          c.getType())
      );
    }

    sb.append("\n위 날씨에 어울리는 옷의 ID를 추천해주세요.");
    return sb.toString();
  }

  private List<UUID> parseIds(String raw) throws Exception {
    JsonNode root = objectMapper.readTree(raw);
    String content = root.path("choices").get(0).path("message").path("content").asText();
    JsonNode contentNode = objectMapper.readTree(content);

    List<UUID> ids = new ArrayList<>();
    for (JsonNode node : contentNode.path("recommendedIds")) {
      try {
        ids.add(UUID.fromString(node.asText()));
      } catch (IllegalArgumentException e) {
        log.warn("[OpenAiClient] 잘못된 UUID 스킵: {}", node.asText());
      }
    }
    return ids;
  }
}
