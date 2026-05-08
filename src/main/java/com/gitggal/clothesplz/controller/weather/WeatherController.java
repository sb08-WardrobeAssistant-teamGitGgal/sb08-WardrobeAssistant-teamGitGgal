package com.gitggal.clothesplz.controller.weather;

import com.gitggal.clothesplz.dto.weather.WeatherDto;
import com.gitggal.clothesplz.dto.weather.WeatherAPILocationDto;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.WeatherErrorCode;
import com.gitggal.clothesplz.service.weather.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/weathers")
public class WeatherController {

    private final WeatherService weatherService;


    @GetMapping
    public Mono<List<WeatherDto>> getWeather(
            @RequestParam(name = "latitude") double latitude,
            @RequestParam(name = "longitude") double longitude) {

        log.info("[Controller] 날씨 조회 요청 - lat: {}, lon: {}", latitude, longitude);

        return weatherService.getWeatherForecast(latitude, longitude)
                .doOnSuccess(res -> log.info("[Controller] 날씨 조회 성공 - count: {}", res.size()))
                .doOnError(e -> log.error("[Controller] 날씨 조회 실패: {}", e.getMessage()))
                .onErrorMap(e -> new BusinessException(WeatherErrorCode.WEATHER_API_ERROR));
    }

    @GetMapping("/location")
    public Mono<WeatherAPILocationDto> getWeatherLocation(
            @RequestParam(name = "latitude") double latitude,
            @RequestParam(name = "longitude") double longitude) {
        log.info("[Controller] 날씨 위치 조회 요청 - lat: {}, lon: {}", latitude, longitude);
        return Mono.fromSupplier(() -> weatherService.getWeatherLocation(latitude, longitude))
                .doOnSuccess(res -> log.info("[Controller] 날씨 위치 조회 성공 - x: {}, y: {}", res.getX(), res.getY()))
                .doOnError(e -> log.error("[Controller] 날씨 위치 조회 실패: {}", e.getMessage()))
                .onErrorMap(e -> new BusinessException(WeatherErrorCode.WEATHER_API_ERROR));
    }
}
