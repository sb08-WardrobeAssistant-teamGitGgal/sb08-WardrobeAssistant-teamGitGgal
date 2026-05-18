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

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/weathers")
public class WeatherController {

    private final WeatherService weatherService;

    // Spring MVC(Tomcat) 환경에서 Mono 반환 대신 .block()으로 동기 처리 — WebFlux 리턴 타입 혼용 문제 해소
    @GetMapping
    public List<WeatherDto> getWeather(
            @RequestParam(name = "latitude") double latitude,
            @RequestParam(name = "longitude") double longitude) {

        log.info("[Controller] 날씨 조회 요청 - lat: {}, lon: {}", latitude, longitude);

        try {
            List<WeatherDto> result = weatherService.getWeatherForecast(latitude, longitude)
                    .doOnNext(res -> log.info("[Controller] 날씨 조회 성공 - count: {}", res.size()))
                    .block();
            return result != null ? result : List.of();
        } catch (Exception e) {
            log.error("[Controller] 날씨 조회 실패: {}", e.getMessage());
            throw new BusinessException(WeatherErrorCode.WEATHER_API_ERROR);
        }
    }

    @GetMapping("/location")
    public WeatherAPILocationDto getWeatherLocation(
            @RequestParam(name = "latitude") double latitude,
            @RequestParam(name = "longitude") double longitude) {

        log.info("[Controller] 날씨 위치 조회 요청 - lat: {}, lon: {}", latitude, longitude);

        try {
            return weatherService.getWeatherLocation(latitude, longitude)
                    .doOnNext(res -> log.info("[Controller] 날씨 위치 조회 성공 - x: {}, y: {}", res.x(), res.y()))
                    .block();
        } catch (Exception e) {
            log.error("[Controller] 날씨 위치 조회 실패: {}", e.getMessage());
            throw new BusinessException(WeatherErrorCode.WEATHER_API_ERROR);
        }
    }
}
