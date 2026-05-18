package com.gitggal.clothesplz.service.weather.impl;

import com.gitggal.clothesplz.dto.weather.DailyWeatherForecastDto;
import com.gitggal.clothesplz.entity.weather.Location;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.entity.weather.WindPhrase;
import com.gitggal.clothesplz.repository.weather.LocationRepository;
import com.gitggal.clothesplz.repository.weather.WeatherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class WeatherPersistenceService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final double WIND_MODERATE_THRESHOLD = 4.0;
    private static final double WIND_STRONG_THRESHOLD = 9.0;

    private final LocationRepository locationRepository;
    private final WeatherRepository weatherRepository;

    // REQUIRES_NEW로 독립 트랜잭션 보장 — 실패 시 외부 트랜잭션 오염 없음
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Location findOrCreateLocation(double lat, double lon, int nx, int ny, String locationNamesStr) {
        return locationRepository.findByGridXAndGridY(nx, ny)
                .orElseGet(() -> locationRepository.save(Location.builder()
                        .latitude(lat).longitude(lon)
                        .gridX(nx).gridY(ny)
                        .locationNames(locationNamesStr)
                        .build()));
    }

    // race condition 발생 시 호출되는 재조회 — 별도 REQUIRES_NEW 트랜잭션으로 rollback-only 상태 회피
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Location findLocationOrThrow(int nx, int ny) {
        return locationRepository.findByGridXAndGridY(nx, ny).orElseThrow();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Weather findOrCreateWeather(Location location, DailyWeatherForecastDto dto) {
        OffsetDateTime forecastAt = dto.date().atStartOfDay().atZone(KST).toOffsetDateTime();
        PrecipitationType precipitationType = dto.precipitationType() == null
                ? PrecipitationType.NONE : dto.precipitationType();

        return weatherRepository.findFirstByLocationAndForecastAt(location, forecastAt)
                .orElseGet(() -> weatherRepository.save(Weather.builder()
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
                        .build()));
    }

    private WindPhrase toWindPhrase(Double speed) {
        if (speed == null || speed < WIND_MODERATE_THRESHOLD) return WindPhrase.WEAK;
        if (speed < WIND_STRONG_THRESHOLD) return WindPhrase.MODERATE;
        return WindPhrase.STRONG;
    }
}
