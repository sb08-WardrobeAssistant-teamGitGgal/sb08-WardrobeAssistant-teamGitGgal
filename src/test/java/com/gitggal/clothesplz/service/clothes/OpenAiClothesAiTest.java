package com.gitggal.clothesplz.service.clothes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.entity.clothes.Clothes;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.weather.Location;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.SkyStatus;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.entity.weather.WindPhrase;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeRepository;
import com.gitggal.clothesplz.service.ai.impl.OpenAiClothesAi;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAiClothesAi 테스트")
class OpenAiClothesAiTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ChatClient.Builder chatClientBuilder;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ChatClient chatClient;

  @Mock
  private ClothesAttributeRepository clothesAttributeRepository;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private OpenAiClothesAi openAiClothesAi;

  @BeforeEach
  void setUp() {
    when(chatClientBuilder.build()).thenReturn(chatClient);
    when(clothesAttributeRepository.findAllByClothesIdIn(any())).thenReturn(List.of());
    openAiClothesAi = new OpenAiClothesAi(chatClientBuilder, objectMapper, clothesAttributeRepository);
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

    when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
        .thenReturn("""
            {"recommendedIds":["%s","%s"]}
            """.formatted(first, second));

    List<UUID> result = openAiClothesAi.recommendClothesIds(weather, allClothes);

    assertThat(result).containsExactly(first, second);
  }

  @Test
  @DisplayName("성공 - 응답에 잘못된 UUID가 포함되면 스킵하고 유효한 ID만 반환한다")
  void recommendClothesIds_invalidUuid_skipsInvalidAndReturnsValid() {
    Weather weather = createWeather(15.0);
    User owner = new User("owner2", "owner2@test.com", "pw");
    Clothes top = createClothes(owner, UUID.randomUUID(), "셔츠", ClothesType.TOP);
    List<Clothes> allClothes = List.of(top);
    UUID valid = top.getId();

    when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
        .thenReturn("""
            {"recommendedIds":["not-a-uuid","%s"]}
            """.formatted(valid));

    List<UUID> result = openAiClothesAi.recommendClothesIds(weather, allClothes);

    assertThat(result).containsExactly(valid);
  }

  @Test
  @DisplayName("실패 - OpenAI 응답 파싱에 실패하면 빈 리스트를 반환한다")
  void recommendClothesIds_parseFail_returnsEmpty() {
    Weather weather = createWeather(18.0);
    User owner = new User("owner3", "owner3@test.com", "pw");
    Clothes outer = createClothes(owner, UUID.randomUUID(), "자켓", ClothesType.OUTER);

    when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
        .thenReturn("not-json");

    List<UUID> result = openAiClothesAi.recommendClothesIds(weather, List.of(outer));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("실패 - OpenAI 호출 중 예외가 발생하면 빈 리스트를 반환한다")
  void recommendClothesIds_callError_returnsEmpty() {
    Weather weather = createWeather(11.0);
    User owner = new User("owner4", "owner4@test.com", "pw");
    Clothes top = createClothes(owner, UUID.randomUUID(), "맨투맨", ClothesType.TOP);

    when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
        .thenThrow(new RuntimeException("timeout"));

    List<UUID> result = openAiClothesAi.recommendClothesIds(weather, List.of(top));

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
