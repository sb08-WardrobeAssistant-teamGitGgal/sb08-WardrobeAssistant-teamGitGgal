package com.gitggal.clothesplz.service.weather;

import com.gitggal.clothesplz.dto.weather.WeatherDto;
import com.gitggal.clothesplz.dto.weather.WeatherAPILocationDto;
import com.gitggal.clothesplz.mapper.weather.WeatherMapper;
import com.gitggal.clothesplz.util.weather.KmaGridCoordinateConverter;
import com.gitggal.clothesplz.util.weather.KmaGridCoordinateConverter.KmaGridPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final WeatherApiService weatherApiService;
    private final WeatherParserService weatherParserService;
    private final WeatherMapper weatherMapper;

    public Mono<List<WeatherDto>> getWeatherForecast(double latitude, double longitude) {
        KmaGridPoint grid = KmaGridCoordinateConverter.toGrid(latitude, longitude);
        log.info(
                "[Service] 기상청 데이터 수집 및 가공 시작: lat={}, lon={}, nx={}, ny={}",
                latitude,
                longitude,
                grid.nx(),
                grid.ny());

        return weatherApiService
                .fetchWeather(grid.nx(), grid.ny())
                .map(
                        response -> {
                            var daily = weatherParserService.parseDailyForecast(response);
                            List<WeatherDto> mapped =
                                    weatherMapper.toWeatherDtoList(
                                            daily, latitude, longitude, grid.nx(), grid.ny());
                            log.info("[Service] 기상청 데이터 가공 완료: 결과 건수={}", mapped.size());
                            return mapped;
                        })
                .doOnError(e -> log.error("[Service] 기상청 데이터 처리 중 에러 발생: {}", e.getMessage()));
    }

    public WeatherAPILocationDto getWeatherLocation(double latitude, double longitude) {
        KmaGridPoint grid = KmaGridCoordinateConverter.toGrid(latitude, longitude);
        return weatherMapper.toLocationDto(latitude, longitude, grid.nx(), grid.ny());
    }
}
