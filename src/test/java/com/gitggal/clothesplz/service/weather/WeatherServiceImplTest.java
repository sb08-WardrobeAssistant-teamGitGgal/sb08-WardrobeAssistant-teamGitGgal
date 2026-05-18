package com.gitggal.clothesplz.service.weather;

import com.gitggal.clothesplz.dto.weather.DailyWeatherForecastDto;
import com.gitggal.clothesplz.dto.weather.WeatherApiResponseDto;
import com.gitggal.clothesplz.dto.weather.WeatherDto;
import com.gitggal.clothesplz.dto.weather.WeatherAPILocationDto;
import com.gitggal.clothesplz.entity.weather.Location;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.mapper.weather.WeatherMapper;
import com.gitggal.clothesplz.repository.weather.LocationRepository;
import com.gitggal.clothesplz.repository.weather.WeatherRepository;
import com.gitggal.clothesplz.service.weather.impl.WeatherServiceImpl;
import com.gitggal.clothesplz.util.weather.KmaGridCoordinateConverter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = WeatherServiceImpl.class)
@ActiveProfiles("test")
class WeatherServiceImplTest {

    @MockitoBean
    private WeatherApiService weatherApiService;

    @MockitoBean
    private WeatherParserService weatherParserService;

    @MockitoBean
    private KakaoLocalApiService kakaoLocalApiService;

    @MockitoBean
    private WeatherMapper weatherMapper;

    @MockitoBean
    private LocationRepository locationRepository;

    @MockitoBean
    private WeatherRepository weatherRepository;

    @MockitoBean
    private TransactionTemplate transactionTemplate;

    @Autowired
    private WeatherServiceImpl weatherServiceImpl;

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
                        null, null, null, null, null)
        );

        KmaGridCoordinateConverter.KmaGridPoint point = KmaGridCoordinateConverter.toGrid(latitude, longitude);

        Location mockLocation = mock(Location.class);
        Weather mockWeather = mock(Weather.class);
        when(mockWeather.getForecastedAt()).thenReturn(OffsetDateTime.now());
        when(mockWeather.getForecastAt()).thenReturn(OffsetDateTime.now());

        when(weatherApiService.fetchWeather(point.nx(), point.ny())).thenReturn(Mono.just(apiResponse));
        when(kakaoLocalApiService.getLocationNames(latitude, longitude)).thenReturn(Mono.just(List.of()));
        when(weatherParserService.parseDailyForecast(apiResponse)).thenReturn(parsed);
        when(locationRepository.findByGridXAndGridY(point.nx(), point.ny())).thenReturn(Optional.of(mockLocation));
        when(weatherRepository.findByLocationAndForecastAt(any(), any())).thenReturn(Optional.of(mockWeather));
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> callback = inv.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        when(weatherMapper.toWeatherDtoList(anyList(), eq(latitude), eq(longitude), eq(point.nx()), eq(point.ny()), eq(List.of())))
                .thenReturn(mapped);

        // when
        List<WeatherDto> result = weatherServiceImpl.getWeatherForecast(latitude, longitude).block();

        // then
        assertThat(result).isEqualTo(mapped);
        verify(weatherApiService).fetchWeather(point.nx(), point.ny());
        verify(weatherParserService).parseDailyForecast(apiResponse);
        verify(weatherMapper).toWeatherDtoList(anyList(), eq(latitude), eq(longitude), eq(point.nx()), eq(point.ny()), eq(List.of()));
    }

    @Test
    @DisplayName("외부 API 예외는 그대로 전파한다")
    void getWeatherForecast_propagatesError() {
        // given
        RuntimeException expected = new RuntimeException("api failed");
        when(weatherApiService.fetchWeather(anyInt(), anyInt())).thenReturn(Mono.error(expected));
        when(kakaoLocalApiService.getLocationNames(anyDouble(), anyDouble())).thenReturn(Mono.just(List.of()));

        // when & then
        assertThatThrownBy(() -> weatherServiceImpl.getWeatherForecast(37.5665, 126.9780).block())
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

        when(kakaoLocalApiService.getLocationNames(latitude, longitude)).thenReturn(Mono.just(List.of()));
        when(weatherMapper.toLocationDto(latitude, longitude, point.nx(), point.ny(), List.of())).thenReturn(locationDto);

        // when
        WeatherAPILocationDto result = weatherServiceImpl.getWeatherLocation(latitude, longitude).block();

        // then
        assertThat(result).isEqualTo(locationDto);
        verify(weatherMapper).toLocationDto(latitude, longitude, point.nx(), point.ny(), List.of());
    }
}
