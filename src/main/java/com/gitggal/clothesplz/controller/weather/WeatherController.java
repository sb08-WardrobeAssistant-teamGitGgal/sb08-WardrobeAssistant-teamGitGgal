package com.gitggal.clothesplz.controller.weather;

import com.gitggal.clothesplz.dto.weather.DailyWeatherForecastDto;
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
@RequestMapping("/api/weather") // 모든 요청은 /api/weather로 시작합니다.
public class WeatherController {

    private final WeatherService weatherService;

    /**
     * 특정 좌표의 날씨 예보 가져오기
     * 예시 호출: GET /api/weather?nx=55&ny=127
     */
    @GetMapping
    public Mono<List<DailyWeatherForecastDto>> getWeather(
            @RequestParam(name = "nx", defaultValue = "55") Integer nx,
            @RequestParam(name = "ny", defaultValue = "127") Integer ny) {

        log.info("[Controller] 날씨 조회 요청 시작 - nx: {}, ny: {}", nx, ny);;

        return weatherService.getWeatherForecast(nx, ny)
                .doOnSuccess(res -> log.info("[Controller] 날씨 조회 성공: 데이터 개수={}", res.size()))
                .doOnError(e -> log.error("[Controller] 날씨 조회 실패: message={}", e.getMessage()));
    }
}