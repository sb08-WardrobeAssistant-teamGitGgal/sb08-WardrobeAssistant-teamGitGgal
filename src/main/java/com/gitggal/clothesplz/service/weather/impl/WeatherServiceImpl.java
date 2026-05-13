package com.gitggal.clothesplz.service.weather.impl;

import com.gitggal.clothesplz.dto.weather.WeatherDto;
import com.gitggal.clothesplz.dto.weather.WeatherAPILocationDto;
import com.gitggal.clothesplz.mapper.weather.WeatherMapper;
import com.gitggal.clothesplz.service.weather.KakaoLocalApiService;
import com.gitggal.clothesplz.service.weather.WeatherApiService;
import com.gitggal.clothesplz.service.weather.WeatherParserService;
import com.gitggal.clothesplz.service.weather.WeatherService;
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
public class WeatherServiceImpl implements WeatherService {

    private final WeatherApiService weatherApiService;
    private final WeatherParserService weatherParserService;
    private final KakaoLocalApiService kakaoLocalApiService;
    private final WeatherMapper weatherMapper;

    public Mono<List<WeatherDto>> getWeatherForecast(double latitude, double longitude) {
        KmaGridPoint grid = KmaGridCoordinateConverter.toGrid(latitude, longitude);
        log.info(
                "[Service] 기상청 데이터 수집 및 가공 시작: lat={}, lon={}, nx={}, ny={}",
                latitude, longitude, grid.nx(), grid.ny());

        return Mono.zip(
                        weatherApiService.fetchWeather(grid.nx(), grid.ny()),
                        kakaoLocalApiService.getLocationNames(latitude, longitude))
                .map(tuple -> {
                    var daily = weatherParserService.parseDailyForecast(tuple.getT1());
                    List<WeatherDto> mapped = weatherMapper.toWeatherDtoList(
                            daily, latitude, longitude, grid.nx(), grid.ny(), tuple.getT2());
                    log.info("[Service] 기상청 데이터 가공 완료: 결과 건수={}", mapped.size());
                    return mapped;
                })
                .doOnError(e -> log.error("[Service] 기상청 데이터 처리 중 에러 발생: {}", e.getMessage()));
    }

    public Mono<WeatherAPILocationDto> getWeatherLocation(double latitude, double longitude) {
        KmaGridPoint grid = KmaGridCoordinateConverter.toGrid(latitude, longitude);
        return kakaoLocalApiService.getLocationNames(latitude, longitude)
                .map(names -> weatherMapper.toLocationDto(latitude, longitude, grid.nx(), grid.ny(), names));
    }
}
