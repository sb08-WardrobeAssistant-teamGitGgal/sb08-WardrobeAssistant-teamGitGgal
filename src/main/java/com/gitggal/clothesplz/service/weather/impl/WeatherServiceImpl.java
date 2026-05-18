package com.gitggal.clothesplz.service.weather.impl;

import com.gitggal.clothesplz.dto.weather.DailyWeatherForecastDto;
import com.gitggal.clothesplz.dto.weather.WeatherDto;
import com.gitggal.clothesplz.dto.weather.WeatherAPILocationDto;
import com.gitggal.clothesplz.entity.weather.Location;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.entity.weather.WindPhrase;
import com.gitggal.clothesplz.mapper.weather.WeatherMapper;
import com.gitggal.clothesplz.repository.weather.LocationRepository;
import com.gitggal.clothesplz.repository.weather.WeatherRepository;
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
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherServiceImpl implements WeatherService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final double WIND_MODERATE_THRESHOLD = 4.0;
    private static final double WIND_STRONG_THRESHOLD = 9.0;

    private final WeatherApiService weatherApiService;
    private final WeatherParserService weatherParserService;
    private final KakaoLocalApiService kakaoLocalApiService;
    private final WeatherMapper weatherMapper;
    private final LocationRepository locationRepository;
    private final WeatherRepository weatherRepository;
    // @Transactional 대신 사용 — publishOn으로 스레드 전환 시 thread-local 트랜잭션 컨텍스트가 끊기는 문제 해결
    private final TransactionTemplate transactionTemplate;

    @Override
    public Mono<List<WeatherDto>> getWeatherForecast(double latitude, double longitude) {
        KmaGridPoint grid = KmaGridCoordinateConverter.toGrid(latitude, longitude);
        log.info("[Service] 기상청 데이터 수집 및 가공 시작: lat={}, lon={}, nx={}, ny={}",
                latitude, longitude, grid.nx(), grid.ny());

        return Mono.zip(
                        weatherApiService.fetchWeather(grid.nx(), grid.ny()),
                        kakaoLocalApiService.getLocationNames(latitude, longitude))
                // 블로킹 DB 작업을 boundedElastic 스레드에서 실행하고 TransactionTemplate으로 트랜잭션 보장
                .flatMap(tuple -> Mono.fromCallable(() ->
                        transactionTemplate.execute(status -> {
                            List<DailyWeatherForecastDto> daily = weatherParserService.parseDailyForecast(tuple.getT1());
                            List<String> locationNames = tuple.getT2();

                            String locationNamesStr = String.join(",", locationNames);
                            Location location = findOrCreateLocation(latitude, longitude, grid.nx(), grid.ny(), locationNamesStr);

                            List<Weather> weathers = daily.stream()
                                    .map(dto -> findOrCreateWeather(location, dto))
                                    .toList();

                            List<WeatherDto> result = weatherMapper.toWeatherDtoList(
                                    weathers, latitude, longitude, grid.nx(), grid.ny(), locationNames);

                            log.info("[Service] 기상청 데이터 가공 완료: 결과 건수={}", result.size());
                            return result;
                        }))
                        .subscribeOn(Schedulers.boundedElastic()))
                .doOnError(e -> log.error("[Service] 기상청 데이터 처리 중 에러 발생: {}", e.getMessage()));
    }

    @Override
    public Mono<WeatherAPILocationDto> getWeatherLocation(double latitude, double longitude) {
        KmaGridPoint grid = KmaGridCoordinateConverter.toGrid(latitude, longitude);
        return kakaoLocalApiService.getLocationNames(latitude, longitude)
                .map(names -> weatherMapper.toLocationDto(latitude, longitude, grid.nx(), grid.ny(), names));
    }

    // 동시 요청 시 unique 제약 위반(uk_grid_x_y)을 catch하고 재조회하여 race condition 방어
    private Location findOrCreateLocation(double lat, double lon, int nx, int ny, String locationNamesStr) {
        return locationRepository.findByGridXAndGridY(nx, ny).orElseGet(() -> {
            try {
                return locationRepository.save(Location.builder()
                        .latitude(lat).longitude(lon)
                        .gridX(nx).gridY(ny)
                        .locationNames(locationNamesStr)
                        .build());
            } catch (DataIntegrityViolationException e) {
                return locationRepository.findByGridXAndGridY(nx, ny).orElseThrow();
            }
        });
    }

    // 기존 배치 저장 데이터 우선 조회, 없으면 저장 — 동시 요청 race condition도 동일하게 방어
    private Weather findOrCreateWeather(Location location, DailyWeatherForecastDto dto) {
        OffsetDateTime forecastAt = dto.date().atStartOfDay().atZone(KST).toOffsetDateTime();
        PrecipitationType precipitationType = dto.precipitationType() == null
                ? PrecipitationType.NONE : dto.precipitationType();

        return weatherRepository.findByLocationAndForecastAt(location, forecastAt).orElseGet(() -> {
            try {
                return weatherRepository.save(Weather.builder()
                        .location(location)
                        .forecastedAt(OffsetDateTime.now(KST))
                        .forecastAt(forecastAt)
                        .skyStatus(dto.skyStatus())
                        .precipitationType(precipitationType)
                        .precipitationAmount(dto.precipitationAmount() != null ? dto.precipitationAmount() : 0.0)
                        .precipitationProbability(dto.precipitationProbability() != null ? dto.precipitationProbability() : 0.0)
                        .humidity(dto.humidityCurrent() != null ? dto.humidityCurrent() : 0.0)
                        .humidityDiff(dto.humidityComparedToDayBefore() != null ? dto.humidityComparedToDayBefore() : 0.0)
                        .temperatureCurrent(dto.avgTemp() != null ? dto.avgTemp() : 0.0)
                        .temperatureDiff(dto.temperatureComparedToDayBefore() != null ? dto.temperatureComparedToDayBefore() : 0.0)
                        .temperatureMin(dto.minTemp() != null ? dto.minTemp() : 0.0)
                        .temperatureMax(dto.maxTemp() != null ? dto.maxTemp() : 0.0)
                        .windSpeed(dto.windSpeed() != null ? dto.windSpeed() : 0.0)
                        .windPhrase(toWindPhrase(dto.windSpeed()))
                        .build());
            } catch (DataIntegrityViolationException e) {
                return weatherRepository.findByLocationAndForecastAt(location, forecastAt).orElseThrow();
            }
        });
    }

    private WindPhrase toWindPhrase(Double speed) {
        if (speed == null || speed < WIND_MODERATE_THRESHOLD) return WindPhrase.WEAK;
        if (speed < WIND_STRONG_THRESHOLD) return WindPhrase.MODERATE;
        return WindPhrase.STRONG;
    }
}
