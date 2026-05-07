package com.gitggal.clothesplz.service.weather;

import com.gitggal.clothesplz.dto.weather.DailyWeatherForecastDto;
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

    public Mono<List<DailyWeatherForecastDto>> getWeatherForecast(Integer gridX, Integer gridY) {
        log.info("[Service] 기상청 데이터 수집 및 가공 시작: nx={}, ny={}", gridX, gridY);

        return weatherApiService.fetchWeather(gridX, gridY)
                .map(response -> {
                    List<DailyWeatherForecastDto> result = weatherParserService.parseDailyForecast(response);
                    log.info("[Service] 기상청 데이터 가공 완료: 결과 건수={}", result.size());
                    return result;
                })
                .doOnError(e -> log.error("[Service] 기상청 데이터 처리 중 에러 발생: {}", e.getMessage()));
    }
}