package com.gitggal.clothesplz.service.weather.impl;

import com.gitggal.clothesplz.dto.weather.WeatherDto;
import com.gitggal.clothesplz.dto.weather.WeatherAPILocationDto;
import com.gitggal.clothesplz.entity.weather.Location;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.mapper.weather.WeatherMapper;
import com.gitggal.clothesplz.service.weather.KakaoLocalApiService;
import com.gitggal.clothesplz.service.weather.WeatherApiService;
import com.gitggal.clothesplz.service.weather.WeatherParserService;
import com.gitggal.clothesplz.service.weather.WeatherService;
import com.gitggal.clothesplz.util.weather.KmaGridCoordinateConverter;
import com.gitggal.clothesplz.util.weather.KmaGridCoordinateConverter.KmaGridPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherServiceImpl implements WeatherService {

    private final WeatherApiService weatherApiService;
    private final WeatherParserService weatherParserService;
    private final KakaoLocalApiService kakaoLocalApiService;
    private final WeatherMapper weatherMapper;
    private final WeatherPersistenceService weatherPersistenceService;

    @Override
    public Mono<List<WeatherDto>> getWeatherForecast(double latitude, double longitude) {
        KmaGridPoint grid = KmaGridCoordinateConverter.toGrid(latitude, longitude);
        log.info("[Service] 기상청 데이터 수집 및 가공 시작: lat={}, lon={}, nx={}, ny={}",
                latitude, longitude, grid.nx(), grid.ny());

        return Mono.zip(
                        weatherApiService.fetchWeather(grid.nx(), grid.ny()),
                        kakaoLocalApiService.getLocationNames(latitude, longitude))
                // 각 find-or-create가 REQUIRES_NEW 독립 트랜잭션으로 실행되므로 TransactionTemplate 불필요
                .flatMap(tuple -> Mono.fromCallable(() -> {
                    var daily = weatherParserService.parseDailyForecast(tuple.getT1());
                    List<String> locationNames = tuple.getT2();
                    String locationNamesStr = String.join(",", locationNames);

                    Location location = findOrCreateLocationSafely(latitude, longitude, grid.nx(), grid.ny(), locationNamesStr);

                    List<Weather> weathers = daily.stream()
                            .map(dto -> weatherPersistenceService.findOrCreateWeather(location, dto))
                            .toList();

                    List<WeatherDto> result = weatherMapper.toWeatherDtoList(
                            weathers, latitude, longitude, grid.nx(), grid.ny(), locationNames);

                    log.info("[Service] 기상청 데이터 가공 완료: 결과 건수={}", result.size());
                    return result;
                }).subscribeOn(Schedulers.boundedElastic()))
                .doOnError(e -> log.error("[Service] 기상청 데이터 처리 중 에러 발생: {}", e.getMessage()));
    }

    @Override
    public Mono<WeatherAPILocationDto> getWeatherLocation(double latitude, double longitude) {
        KmaGridPoint grid = KmaGridCoordinateConverter.toGrid(latitude, longitude);
        return kakaoLocalApiService.getLocationNames(latitude, longitude)
                .map(names -> weatherMapper.toLocationDto(latitude, longitude, grid.nx(), grid.ny(), names));
    }

    // REQUIRES_NEW 트랜잭션 실패(동시 insert 충돌) 시 별도 트랜잭션으로 재조회
    private Location findOrCreateLocationSafely(double lat, double lon, int nx, int ny, String locationNamesStr) {
        try {
            return weatherPersistenceService.findOrCreateLocation(lat, lon, nx, ny, locationNamesStr);
        } catch (DataIntegrityViolationException e) {
            return weatherPersistenceService.findLocationOrThrow(nx, ny);
        }
    }
}
