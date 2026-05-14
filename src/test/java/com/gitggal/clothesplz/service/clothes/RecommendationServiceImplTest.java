package com.gitggal.clothesplz.service.clothes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.gitggal.clothesplz.dto.clothes.ClothesDto;
import com.gitggal.clothesplz.dto.clothes.RecommendationDto;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.clothes.Clothes;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.user.UserRole;
import com.gitggal.clothesplz.entity.weather.Location;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.SkyStatus;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.entity.weather.WindPhrase;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.CommonErrorCode;
import com.gitggal.clothesplz.exception.code.WeatherErrorCode;
import com.gitggal.clothesplz.mapper.clothes.ClothesMapper;
import com.gitggal.clothesplz.repository.clothes.ClothesRepository;
import com.gitggal.clothesplz.repository.weather.WeatherRepository;
import com.gitggal.clothesplz.service.clothes.impl.RecommendationServiceImpl;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService 비즈니스 로직 테스트")
class RecommendationServiceImplTest {

  @Mock
  private WeatherRepository weatherRepository;

  @Mock
  private ClothesRepository clothesRepository;

  @Mock
  private ClothesMapper clothesMapper;

  @Mock
  private OpenAiClient openAiClient;

  @InjectMocks
  private RecommendationServiceImpl recommendationService;

  @Test
  @DisplayName("성공 - LLM 추천 ID 순서대로 의상 추천 결과를 반환한다")
  void getRecommendations_returnsLlmResultInOrder() {
    UUID weatherId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UserDto user = createUserDto(userId);
    Weather weather = createWeather(18.0);
    User owner = new User("owner", "owner@test.com", "pw");

    UUID topId = UUID.randomUUID();
    UUID outerId = UUID.randomUUID();
    Clothes top = createClothes(owner, topId, "반팔", ClothesType.TOP);
    Clothes outer = createClothes(owner, outerId, "바람막이", ClothesType.OUTER);
    List<Clothes> allClothes = List.of(top, outer);

    ClothesDto topDto = new ClothesDto(topId, userId, "반팔", null, ClothesType.TOP, List.of());
    ClothesDto outerDto = new ClothesDto(outerId, userId, "바람막이", null, ClothesType.OUTER, List.of());

    given(weatherRepository.findById(weatherId)).willReturn(Optional.of(weather));
    given(clothesRepository.findByOwnerId(userId)).willReturn(allClothes);
    given(openAiClient.recommendClothesIds(weather, allClothes)).willReturn(List.of(outerId, topId));
    given(clothesMapper.toClothesDto(outer, owner, List.of())).willReturn(outerDto);
    given(clothesMapper.toClothesDto(top, owner, List.of())).willReturn(topDto);

    RecommendationDto result = recommendationService.getRecommendations(weatherId.toString(), user);

    assertThat(result.weatherId()).isEqualTo(weatherId.toString());
    assertThat(result.userId()).isEqualTo(userId.toString());
    assertThat(result.clothes()).containsExactly(outerDto, topDto);
  }

  @Test
  @DisplayName("실패 - weatherId 형식이 잘못되면 INVALID_INPUT 예외를 던진다")
  void getRecommendations_invalidWeatherId_throwsInvalidInput() {
    UserDto user = createUserDto(UUID.randomUUID());

    BusinessException exception = catchThrowableOfType(
        () -> recommendationService.getRecommendations("bad-weather-id", user),
        BusinessException.class
    );

    assertThat(exception).isNotNull();
    assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT);
    verifyNoInteractions(weatherRepository, clothesRepository, clothesMapper, openAiClient);
  }

  @Test
  @DisplayName("실패 - weather가 없으면 WEATHER_NOT_FOUND 예외를 던진다")
  void getRecommendations_weatherNotFound_throwsWeatherNotFound() {
    UUID weatherId = UUID.randomUUID();
    UserDto user = createUserDto(UUID.randomUUID());
    given(weatherRepository.findById(weatherId)).willReturn(Optional.empty());

    BusinessException exception = catchThrowableOfType(
        () -> recommendationService.getRecommendations(weatherId.toString(), user),
        BusinessException.class
    );

    assertThat(exception).isNotNull();
    assertThat(exception.getErrorCode()).isEqualTo(WeatherErrorCode.WEATHER_NOT_FOUND);
    verify(weatherRepository).findById(weatherId);
    verifyNoInteractions(clothesRepository, clothesMapper, openAiClient);
  }

  @Test
  @DisplayName("성공 - LLM 추천이 없으면 온도 기반 fallback 결과를 반환한다")
  void getRecommendations_llmEmpty_returnsFallback() {
    UUID weatherId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UserDto user = createUserDto(userId);
    Weather weather = createWeather(27.0);
    User owner = new User("owner2", "owner2@test.com", "pw");

    UUID topId = UUID.randomUUID();
    UUID outerId = UUID.randomUUID();
    Clothes top = createClothes(owner, topId, "민소매", ClothesType.TOP);
    Clothes outer = createClothes(owner, outerId, "코트", ClothesType.OUTER);
    List<Clothes> allClothes = List.of(top, outer);

    ClothesDto topDto = new ClothesDto(topId, userId, "민소매", null, ClothesType.TOP, List.of());

    given(weatherRepository.findById(weatherId)).willReturn(Optional.of(weather));
    given(clothesRepository.findByOwnerId(userId)).willReturn(allClothes);
    given(openAiClient.recommendClothesIds(weather, allClothes)).willReturn(List.of());
    given(clothesMapper.toClothesDto(top, owner, List.of())).willReturn(topDto);

    RecommendationDto result = recommendationService.getRecommendations(weatherId.toString(), user);

    assertThat(result.clothes()).containsExactly(topDto);
    verify(openAiClient).recommendClothesIds(weather, allClothes);
  }

  private UserDto createUserDto(UUID userId) {
    return new UserDto(
        userId,
        Instant.now(),
        "test@test.com",
        "tester",
        UserRole.USER,
        false
    );
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
        .precipitationProbability(0.0)
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
