package com.gitggal.clothesplz.mapper.weather;

import com.gitggal.clothesplz.dto.weather.DailyWeatherForecastDto;
import com.gitggal.clothesplz.dto.weather.WeatherAPILocationDto;
import com.gitggal.clothesplz.dto.weather.WeatherDto;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.SkyStatus;
import com.gitggal.clothesplz.entity.weather.WindPhrase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WeatherMapper 단위 테스트")
@ExtendWith(MockitoExtension.class)
class WeatherMapperTest {

    @InjectMocks
    private WeatherMapper weatherMapper;

    // ── toWindPhrase ──────────────────────────────────────────────────────

    @ParameterizedTest(name = "풍속 {0} m/s → {1}")
    @CsvSource({
            "0.0,  WEAK",
            "3.9,  WEAK",
            "4.0,  MODERATE",
            "8.9,  MODERATE",
            "9.0,  STRONG",
            "15.0, STRONG"
    })
    @DisplayName("풍속 구간별로 올바른 WindPhrase를 반환한다")
    void toWindPhrase_bySpeed(double speed, WindPhrase expected) {
        WeatherDto result = weatherMapper.toWeatherDto(
                forecast(speed), 37.5, 127.0, 60, 127, List.of());
        assertThat(result.windSpeed().asWord()).isEqualTo(expected);
    }

    @Test
    @DisplayName("풍속이 null이면 WEAK을 반환한다")
    void toWindPhrase_nullSpeed_returnsWeak() {
        WeatherDto result = weatherMapper.toWeatherDto(
                forecast(null), 37.5, 127.0, 60, 127, List.of());
        assertThat(result.windSpeed().asWord()).isEqualTo(WindPhrase.WEAK);
    }

    // ── stableWeatherId ───────────────────────────────────────────────────

    @Test
    @DisplayName("같은 격자 좌표와 날짜면 항상 동일한 UUID를 반환한다")
    void stableWeatherId_sameInput_returnsSameUUID() {
        DailyWeatherForecastDto dto = forecast(1.0);

        WeatherDto first  = weatherMapper.toWeatherDto(dto, 37.5, 127.0, 60, 127, List.of());
        WeatherDto second = weatherMapper.toWeatherDto(dto, 37.5, 127.0, 60, 127, List.of());

        assertThat(first.id()).isEqualTo(second.id());
    }

    @Test
    @DisplayName("격자 좌표가 다르면 다른 UUID를 반환한다")
    void stableWeatherId_differentGrid_returnsDifferentUUID() {
        DailyWeatherForecastDto dto = forecast(1.0);

        UUID id1 = weatherMapper.toWeatherDto(dto, 37.5, 127.0, 60, 127, List.of()).id();
        UUID id2 = weatherMapper.toWeatherDto(dto, 37.5, 127.0, 61, 128, List.of()).id();

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("날짜가 다르면 다른 UUID를 반환한다")
    void stableWeatherId_differentDate_returnsDifferentUUID() {
        DailyWeatherForecastDto today    = forecast(LocalDate.of(2026, 5, 12), 1.0);
        DailyWeatherForecastDto tomorrow = forecast(LocalDate.of(2026, 5, 13), 1.0);

        UUID id1 = weatherMapper.toWeatherDto(today,    37.5, 127.0, 60, 127, List.of()).id();
        UUID id2 = weatherMapper.toWeatherDto(tomorrow, 37.5, 127.0, 60, 127, List.of()).id();

        assertThat(id1).isNotEqualTo(id2);
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
        List<DailyWeatherForecastDto> forecasts = List.of(
                forecast(LocalDate.of(2026, 5, 12), 1.0),
                forecast(LocalDate.of(2026, 5, 13), 5.0),
                forecast(LocalDate.of(2026, 5, 14), 10.0)
        );

        List<WeatherDto> result = weatherMapper.toWeatherDtoList(
                forecasts, 37.5, 127.0, 60, 127, List.of("서울특별시", "중구", "을지로동"));

        assertThat(result).hasSize(3);
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────

    private DailyWeatherForecastDto forecast(Double windSpeed) {
        return forecast(LocalDate.of(2026, 5, 12), windSpeed);
    }

    private DailyWeatherForecastDto forecast(LocalDate date, Double windSpeed) {
        return new DailyWeatherForecastDto(
                date, SkyStatus.CLEAR, 16.0, 11.0, 20.0,
                42.0, 0.0, PrecipitationType.NONE, 0.0, 0.0, windSpeed, 0.0);
    }
}
