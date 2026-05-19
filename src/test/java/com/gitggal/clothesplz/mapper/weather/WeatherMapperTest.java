package com.gitggal.clothesplz.mapper.weather;

import com.gitggal.clothesplz.dto.weather.WeatherAPILocationDto;
import com.gitggal.clothesplz.dto.weather.WeatherDto;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.SkyStatus;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.entity.weather.WindPhrase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("WeatherMapper 단위 테스트")
@ExtendWith(MockitoExtension.class)
class WeatherMapperTest {

    @InjectMocks
    private WeatherMapper weatherMapper;

    // ── windPhrase passthrough ────────────────────────────────────────────

    @ParameterizedTest(name = "WindPhrase {0}이 DTO에 그대로 매핑된다")
    @EnumSource(WindPhrase.class)
    @DisplayName("엔티티의 windPhrase가 DTO windSpeed.asWord()에 그대로 전달된다")
    void toWeatherDto_mapsWindPhraseFromEntity(WindPhrase phrase) {
        Weather weather = mockWeather(UUID.randomUUID(), phrase);
        WeatherDto result = weatherMapper.toWeatherDto(weather, 37.5, 127.0, 60, 127, List.of());
        assertThat(result.windSpeed().asWord()).isEqualTo(phrase);
    }

    // ── id passthrough (DB UUID 사용 검증) ──────────────────────────────────

    @Test
    @DisplayName("toWeatherDto는 weather.getId()를 DTO id로 사용한다")
    void toWeatherDto_usesEntityId() {
        UUID expectedId = UUID.randomUUID();
        WeatherDto result = weatherMapper.toWeatherDto(mockWeather(expectedId, WindPhrase.WEAK), 37.5, 127.0, 60, 127, List.of());
        assertThat(result.id()).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("서로 다른 Weather 엔티티는 서로 다른 id의 DTO를 반환한다")
    void toWeatherDto_differentEntities_differentIds() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        WeatherDto dto1 = weatherMapper.toWeatherDto(mockWeather(id1, WindPhrase.WEAK), 37.5, 127.0, 60, 127, List.of());
        WeatherDto dto2 = weatherMapper.toWeatherDto(mockWeather(id2, WindPhrase.WEAK), 37.5, 127.0, 60, 127, List.of());

        assertThat(dto1.id()).isNotEqualTo(dto2.id());
    }

    // ── toLocationDto ─────────────────────────────────────────────────────

    @Test
    @DisplayName("toLocationDto는 전달받은 값을 그대로 담아 반환한다")
    void toLocationDto_mapsFieldsCorrectly() {
        List<String> names = List.of("서울특별시", "중구", "을지로동");

        WeatherAPILocationDto result = weatherMapper.toLocationDto(37.5, 127.0, 60, 127, names);

        assertThat(result.latitude()).isEqualTo(37.5);
        assertThat(result.longitude()).isEqualTo(127.0);
        assertThat(result.x()).isEqualTo(60);
        assertThat(result.y()).isEqualTo(127);
        assertThat(result.locationNames()).isEqualTo(names);
    }

    // ── toWeatherDtoList ──────────────────────────────────────────────────

    @Test
    @DisplayName("toWeatherDtoList는 입력 리스트 크기만큼 WeatherDto를 반환한다")
    void toWeatherDtoList_returnsSameSizeList() {
        List<Weather> weathers = List.of(
                mockWeather(UUID.randomUUID(), WindPhrase.WEAK),
                mockWeather(UUID.randomUUID(), WindPhrase.MODERATE),
                mockWeather(UUID.randomUUID(), WindPhrase.STRONG)
        );

        List<WeatherDto> result = weatherMapper.toWeatherDtoList(
                weathers, 37.5, 127.0, 60, 127, List.of("서울특별시", "중구", "을지로동"));

        assertThat(result).hasSize(3);
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────

    private Weather mockWeather(UUID id, WindPhrase windPhrase) {
        Weather weather = mock(Weather.class);
        when(weather.getId()).thenReturn(id);
        when(weather.getForecastedAt()).thenReturn(OffsetDateTime.now());
        when(weather.getForecastAt()).thenReturn(OffsetDateTime.now());
        when(weather.getSkyStatus()).thenReturn(SkyStatus.CLEAR);
        when(weather.getPrecipitationType()).thenReturn(PrecipitationType.NONE);
        when(weather.getPrecipitationAmount()).thenReturn(0.0);
        when(weather.getPrecipitationProbability()).thenReturn(0.0);
        when(weather.getHumidity()).thenReturn(42.0);
        when(weather.getHumidityDiff()).thenReturn(0.0);
        when(weather.getTemperatureCurrent()).thenReturn(16.0);
        when(weather.getTemperatureDiff()).thenReturn(0.0);
        when(weather.getTemperatureMin()).thenReturn(11.0);
        when(weather.getTemperatureMax()).thenReturn(20.0);
        when(weather.getWindSpeed()).thenReturn(4.0);
        when(weather.getWindPhrase()).thenReturn(windPhrase);
        return weather;
    }
}
