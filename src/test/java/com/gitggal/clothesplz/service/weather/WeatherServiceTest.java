package com.gitggal.clothesplz.service.weather;

import com.gitggal.clothesplz.dto.weather.DailyWeatherForecastDto;
import com.gitggal.clothesplz.dto.weather.WeatherApiResponseDto;
import com.gitggal.clothesplz.dto.weather.WeatherDto;
import com.gitggal.clothesplz.dto.weather.WeatherAPILocationDto;
import com.gitggal.clothesplz.mapper.weather.WeatherMapper;
import com.gitggal.clothesplz.util.weather.KmaGridCoordinateConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = WeatherService.class)
@ActiveProfiles("test")
class WeatherServiceTest {

    @MockitoBean
    private WeatherApiService weatherApiService;

    @MockitoBean
    private WeatherParserService weatherParserService;

    @MockitoBean
    private WeatherMapper weatherMapper;

    @Autowired
    private WeatherService weatherService;

    @Test
    @DisplayName("날씨 예보 조회 시 API 응답을 파싱/매핑해 반환한다")
    void getWeatherForecast_success() {
        // given
        double latitude = 37.5665;
        double longitude = 126.9780;

        WeatherApiResponseDto apiResponse = new WeatherApiResponseDto(null);
        List<DailyWeatherForecastDto> parsed = List.of(
                new DailyWeatherForecastDto(
                        LocalDate.of(2026, 5, 8), null, 16.0, 11.0, 20.0,
                        42.0, -28.0, null, 0.0, 0.0, 4.0, 1.0)
        );
        List<WeatherDto> mapped = List.of(
                new WeatherDto(
                        UUID.randomUUID(),
                        LocalDateTime.now(),
                        LocalDate.of(2026, 5, 8).atStartOfDay(),
                        new WeatherAPILocationDto(latitude, longitude, 60, 127, List.of()),
                        null,
                        null,
                        null,
                        null,
                        null
                )
        );

        KmaGridCoordinateConverter.KmaGridPoint point = KmaGridCoordinateConverter.toGrid(latitude, longitude);

        when(weatherApiService.fetchWeather(point.nx(), point.ny())).thenReturn(Mono.just(apiResponse));
        when(weatherParserService.parseDailyForecast(apiResponse)).thenReturn(parsed);
        when(weatherMapper.toWeatherDtoList(parsed, latitude, longitude, point.nx(), point.ny())).thenReturn(mapped);

        // when & then
        List<WeatherDto> result = weatherService.getWeatherForecast(latitude, longitude).block();

        assertThat(result).isEqualTo(mapped);

        verify(weatherApiService).fetchWeather(point.nx(), point.ny());
        verify(weatherParserService).parseDailyForecast(apiResponse);
        verify(weatherMapper).toWeatherDtoList(parsed, latitude, longitude, point.nx(), point.ny());
    }

    @Test
    @DisplayName("외부 API 예외는 그대로 전파한다")
    void getWeatherForecast_propagatesError() {
        // given
        RuntimeException expected = new RuntimeException("api failed");
        when(weatherApiService.fetchWeather(anyInt(), anyInt())).thenReturn(Mono.error(expected));

        // when & then
        assertThatThrownBy(() -> weatherService.getWeatherForecast(37.5665, 126.9780).block())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("api failed");
    }

    @Test
    @DisplayName("위치 조회는 격자 변환 후 mapper 결과를 반환한다")
    void getWeatherLocation_success() {
        // given
        double latitude = 37.5665;
        double longitude = 126.9780;
        KmaGridCoordinateConverter.KmaGridPoint point = KmaGridCoordinateConverter.toGrid(latitude, longitude);
        WeatherAPILocationDto locationDto = new WeatherAPILocationDto(latitude, longitude, point.nx(), point.ny(), List.of());

        when(weatherMapper.toLocationDto(latitude, longitude, point.nx(), point.ny())).thenReturn(locationDto);

        // when
        WeatherAPILocationDto result = weatherService.getWeatherLocation(latitude, longitude);

        // then
        assertThat(result).isEqualTo(locationDto);
        verify(weatherMapper).toLocationDto(latitude, longitude, point.nx(), point.ny());
    }
}

