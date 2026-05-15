package com.gitggal.clothesplz.service.clothes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.entity.clothes.Clothes;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.weather.Location;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.SkyStatus;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.entity.weather.WindPhrase;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAiClient 테스트")
class OpenAiClientTest {

  @Mock
  private WebClient webClient;

  @Mock
  private WebClient.RequestBodyUriSpec requestBodyUriSpec;

  @Mock
  private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

  @Mock
  private WebClient.ResponseSpec responseSpec;

  private OpenAiClient openAiClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    openAiClient = new OpenAiClient("dummy-openai-key", objectMapper);
    ReflectionTestUtils.setField(openAiClient, "webClient", webClient);
  }

  @Test
  @DisplayName("성공 - 유효한 UUID 응답을 파싱해 추천 ID를 반환한다")
  void recommendClothesIds_success_returnsParsedIds() {
    Weather weather = createWeather(22.0);
    User owner = new User("owner", "owner@test.com", "pw");
    Clothes top = createClothes(owner, UUID.randomUUID(), "반팔", ClothesType.TOP);
    Clothes outer = createClothes(owner, UUID.randomUUID(), "가디건", ClothesType.OUTER);
    List<Clothes> allClothes = List.of(top, outer);

    UUID first = top.getId();
    UUID second = outer.getId();
    String raw = """
        {
          "choices": [
            {
              "message": {
                "content": "{\\"recommendedIds\\":[\\"%s\\",\\"%s\\"]}"
              }
            }
          ]
        }
        """.formatted(first, second);

    when(webClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(raw));

    List<UUID> result = openAiClient.recommendClothesIds(weather, allClothes);

    assertThat(result).containsExactly(first, second);

    ArgumentCaptor<Object> bodyCaptor = ArgumentCaptor.forClass(Object.class);
    verify(requestBodyUriSpec).bodyValue(bodyCaptor.capture());
    JsonNode body = (JsonNode) bodyCaptor.getValue();
    assertThat(body.path("model").asText()).isEqualTo("gpt-4o-mini");
    assertThat(body.path("response_format").path("type").asText()).isEqualTo("json_object");

    String prompt = body.path("messages").get(1).path("content").asText();
    assertThat(prompt).contains(first.toString(), second.toString(), "반팔", "가디건");
  }

  @Test
  @DisplayName("성공 - 응답에 잘못된 UUID가 포함되면 스킵하고 유효한 ID만 반환한다")
  void recommendClothesIds_invalidUuid_skipsInvalidAndReturnsValid() {
    Weather weather = createWeather(15.0);
    User owner = new User("owner2", "owner2@test.com", "pw");
    Clothes top = createClothes(owner, UUID.randomUUID(), "셔츠", ClothesType.TOP);
    List<Clothes> allClothes = List.of(top);

    UUID valid = top.getId();
    String raw = """
        {
          "choices": [
            {
              "message": {
                "content": "{\\"recommendedIds\\":[\\"not-a-uuid\\",\\"%s\\"]}"
              }
            }
          ]
        }
        """.formatted(valid);

    when(webClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(raw));

    List<UUID> result = openAiClient.recommendClothesIds(weather, allClothes);

    assertThat(result).containsExactly(valid);
  }

  @Test
  @DisplayName("실패 - OpenAI 응답 파싱에 실패하면 빈 리스트를 반환한다")
  void recommendClothesIds_parseFail_returnsEmpty() {
    Weather weather = createWeather(18.0);
    User owner = new User("owner3", "owner3@test.com", "pw");
    Clothes outer = createClothes(owner, UUID.randomUUID(), "자켓", ClothesType.OUTER);
    List<Clothes> allClothes = List.of(outer);

    String invalidRaw = "{\"choices\": [{\"message\": {\"content\": \"not-json\"}}]}";

    when(webClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(invalidRaw));

    List<UUID> result = openAiClient.recommendClothesIds(weather, allClothes);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("실패 - OpenAI 호출 중 예외가 발생하면 빈 리스트를 반환한다")
  void recommendClothesIds_httpError_returnsEmpty() {
    Weather weather = createWeather(11.0);
    User owner = new User("owner4", "owner4@test.com", "pw");
    Clothes top = createClothes(owner, UUID.randomUUID(), "맨투맨", ClothesType.TOP);
    List<Clothes> allClothes = List.of(top);

    when(webClient.post()).thenReturn(requestBodyUriSpec);
    when(requestBodyUriSpec.bodyValue(any())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(new RuntimeException("timeout")));

    List<UUID> result = openAiClient.recommendClothesIds(weather, allClothes);

    assertThat(result).isEmpty();
  }

  private Clothes createClothes(User owner, UUID id, String name, ClothesType type) {
    Clothes clothes = new Clothes(owner, name, type, null, null);
    ReflectionTestUtils.setField(clothes, "id", id);
    return clothes;
  }

  private Weather createWeather(double currentTemp) {
    Location location = Location.builder()
        .latitude(37.5)
        .longitude(127.0)
        .gridX(60)
        .gridY(127)
        .locationNames("서울")
        .build();

    return Weather.builder()
        .forecastedAt(OffsetDateTime.now().minusHours(1))
        .forecastAt(OffsetDateTime.now())
        .location(location)
        .skyStatus(SkyStatus.CLEAR)
        .precipitationType(PrecipitationType.NONE)
        .precipitationAmount(0.0)
        .precipitationProbability(10.0)
        .humidity(45.0)
        .humidityDiff(0.0)
        .temperatureCurrent(currentTemp)
        .temperatureDiff(0.0)
        .temperatureMin(currentTemp - 2.0)
        .temperatureMax(currentTemp + 2.0)
        .windSpeed(2.0)
        .windPhrase(WindPhrase.WEAK)
        .build();
  }
}
