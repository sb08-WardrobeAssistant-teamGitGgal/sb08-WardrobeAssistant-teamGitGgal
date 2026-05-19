package com.gitggal.clothesplz.component.batch.weather;

import com.gitggal.clothesplz.dto.weather.DailyWeatherForecastDto;
import com.gitggal.clothesplz.dto.weather.WeatherApiResponseDto;
import com.gitggal.clothesplz.entity.weather.*;
import com.gitggal.clothesplz.service.weather.WeatherApiService;
import com.gitggal.clothesplz.service.weather.WeatherParserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@DisplayName("WeatherFetchProcessor 단위 테스트")
@ExtendWith(MockitoExtension.class)
class WeatherFetchProcessorTest {

    @Mock
    private WeatherApiService weatherApiService;

    @Mock
    private WeatherParserService weatherParserService;

    @InjectMocks
    private WeatherFetchProcessor processor;

    private Location location;

    @BeforeEach
    void setUp() {
        location = Location.builder()
                .latitude(37.5)
                .longitude(127.0)
                .gridX(60)
                .gridY(127)
                .locationNames("서울특별시 중구")
                .build();
    }

    @Nested
    @DisplayName("process()")
    class ProcessTest {

        @Test
        @DisplayName("정상 API 응답 → Weather 리스트 반환")
        void process_validResponse_returnsWeatherList() {
            WeatherApiResponseDto response = mock(WeatherApiResponseDto.class);
            List<DailyWeatherForecastDto> forecasts = List.of(
                    new DailyWeatherForecastDto(
                            LocalDate.now(), SkyStatus.CLEAR,
                            20.0, 15.0, 25.0,
                            60.0, 5.0,
                            PrecipitationType.NONE, 0.0, 10.0,
                            2.0, 1.0
                    )
            );
            given(weatherApiService.fetchWeather(60, 127)).willReturn(Mono.just(response));
            given(weatherParserService.parseDailyForecast(response)).willReturn(forecasts);

            List<Weather> result = processor.process(location);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getLocation()).isEqualTo(location);
        }

        @Test
        @DisplayName("API 응답 null → null 반환 (해당 location skip)")
        void process_nullResponse_returnsNull() {
            given(weatherApiService.fetchWeather(60, 127)).willReturn(Mono.empty());

            List<Weather> result = processor.process(location);

            assertThat(result).isNull();
            verify(weatherParserService, never()).parseDailyForecast(null);
        }

        @Test
        @DisplayName("예보 여러 건 → 동일 개수 Weather 반환")
        void process_multipleForecasts_returnsAllWeathers() {
            WeatherApiResponseDto response = mock(WeatherApiResponseDto.class);
            List<DailyWeatherForecastDto> forecasts = List.of(
                    makeDto(LocalDate.now(), 2.0),
                    makeDto(LocalDate.now().plusDays(1), 5.0),
                    makeDto(LocalDate.now().plusDays(2), 10.0)
            );
            given(weatherApiService.fetchWeather(60, 127)).willReturn(Mono.just(response));
            given(weatherParserService.parseDailyForecast(response)).willReturn(forecasts);

            List<Weather> result = processor.process(location);

            assertThat(result).hasSize(3);
        }
    }

    @Nested
    @DisplayName("WindPhrase 분류")
    class WindPhraseTest {

        @ParameterizedTest(name = "풍속 {0} → {1}")
        @CsvSource({
                "0.0, WEAK",
                "3.9, WEAK",
                "4.0, MODERATE",
                "8.9, MODERATE",
                "9.0, STRONG",
                "15.0, STRONG"
        })
        @DisplayName("풍속 임계값에 따라 WindPhrase 분류")
        void windPhrase_bySpeed(double speed, WindPhrase expected) {
            WeatherApiResponseDto response = mock(WeatherApiResponseDto.class);
            List<DailyWeatherForecastDto> forecasts = List.of(makeDto(LocalDate.now(), speed));
            given(weatherApiService.fetchWeather(60, 127)).willReturn(Mono.just(response));
            given(weatherParserService.parseDailyForecast(response)).willReturn(forecasts);

            List<Weather> result = processor.process(location);

            assertThat(result.get(0).getWindPhrase()).isEqualTo(expected);
        }

        @Test
        @DisplayName("풍속 null → WEAK")
        void windPhrase_nullSpeed_returnsWeak() {
            WeatherApiResponseDto response = mock(WeatherApiResponseDto.class);
            List<DailyWeatherForecastDto> forecasts = List.of(
                    new DailyWeatherForecastDto(
                            LocalDate.now(), SkyStatus.CLEAR,
                            20.0, 15.0, 25.0,
                            60.0, 5.0,
                            PrecipitationType.NONE, 0.0, 10.0,
                            null, 1.0
                    )
            );
            given(weatherApiService.fetchWeather(60, 127)).willReturn(Mono.just(response));
            given(weatherParserService.parseDailyForecast(response)).willReturn(forecasts);

            List<Weather> result = processor.process(location);

            assertThat(result.get(0).getWindPhrase()).isEqualTo(WindPhrase.WEAK);
        }
    }

    private DailyWeatherForecastDto makeDto(LocalDate date, Double windSpeed) {
        return new DailyWeatherForecastDto(
                date, SkyStatus.CLEAR,
                20.0, 15.0, 25.0,
                60.0, 5.0,
                PrecipitationType.NONE, 0.0, 10.0,
                windSpeed, 1.0
        );
    }
}
