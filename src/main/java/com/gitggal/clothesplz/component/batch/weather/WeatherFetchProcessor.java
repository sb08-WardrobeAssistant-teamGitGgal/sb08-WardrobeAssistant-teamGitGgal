package com.gitggal.clothesplz.component.batch.weather;

import com.gitggal.clothesplz.dto.weather.DailyWeatherForecastDto;
import com.gitggal.clothesplz.entity.weather.Location;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.entity.weather.WindPhrase;
import com.gitggal.clothesplz.service.weather.WeatherApiService;
import com.gitggal.clothesplz.service.weather.WeatherParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

// Location별 KMA API 호출 후 Weather 엔티티 리스트로 변환
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherFetchProcessor implements ItemProcessor<Location, List<Weather>> {

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);
    private static final double WIND_MODERATE_THRESHOLD = 4.0;
    private static final double WIND_STRONG_THRESHOLD = 9.0;

    private final WeatherApiService weatherApiService;
    private final WeatherParserService weatherParserService;

    @Override
    public List<Weather> process(Location location) {
        log.info("[Batch] 날씨 데이터 수집: gridX={}, gridY={}", location.getGridX(), location.getGridY());

        var response = weatherApiService.fetchWeather(location.getGridX(), location.getGridY()).block();
        if (response == null) {
            log.warn("[Batch] API 응답 없음: gridX={}, gridY={}", location.getGridX(), location.getGridY());
            return null;
        }
        List<DailyWeatherForecastDto> forecasts = weatherParserService.parseDailyForecast(response);

        return forecasts.stream()
                .map(dto -> toWeather(dto, location))
                .toList();
    }

    private Weather toWeather(DailyWeatherForecastDto dto, Location location) {
        return Weather.builder()
                .forecastedAt(OffsetDateTime.now(KST))
                .forecastAt(toOffsetDateTime(dto.date()))
                .location(location)
                .skyStatus(dto.skyStatus())
                .precipitationType(dto.precipitationType() != null ? dto.precipitationType() : PrecipitationType.NONE)
                .precipitationAmount(dto.precipitationAmount())
                .precipitationProbability(dto.precipitationProbability())
                .humidity(dto.humidityCurrent())
                .humidityDiff(dto.humidityComparedToDayBefore())
                .temperatureCurrent(dto.avgTemp())
                .temperatureDiff(dto.temperatureComparedToDayBefore())
                .temperatureMin(dto.minTemp())
                .temperatureMax(dto.maxTemp())
                .windSpeed(dto.windSpeed())
                .windPhrase(toWindPhrase(dto.windSpeed()))
                .build();
    }

    private OffsetDateTime toOffsetDateTime(LocalDate date) {
        return date.atStartOfDay().atOffset(KST);
    }

    private WindPhrase toWindPhrase(Double speed) {
        if (speed == null || speed < WIND_MODERATE_THRESHOLD) return WindPhrase.WEAK;
        if (speed < WIND_STRONG_THRESHOLD) return WindPhrase.MODERATE;
        return WindPhrase.STRONG;
    }
}
