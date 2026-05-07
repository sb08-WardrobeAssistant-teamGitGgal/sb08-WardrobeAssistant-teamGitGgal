package com.gitggal.clothesplz.service.weather;

import com.gitggal.clothesplz.dto.weather.WeatherApiResponseDto;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.WeatherErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherApiService {

    private final WebClient webClient;

    // static final 상수로 분리하여 성능 최적화 및 타임존 고정
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");


    @Value("${weather.api.url}")
    private String apiUrl;

    @Value("${weather.api.service-key}")
    private String serviceKey;


    /**
     * 특정 격자 좌표(gridX, gridY)에 대한 날씨 데이터를 가져오는 메서드
     */
    public Mono<WeatherApiResponseDto> fetchWeather(Integer gridX, Integer gridY) {
        // 기상청 발표 시간 기준에 맞게 base_date와 base_time을 계산하여 배열로 반환받음
        String[] baseTimeInfo = calculateBaseTime();

        // 안전한 API 호출을 위해 URL과 파라미터를 조합하여 URI 객체 생성
        URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("serviceKey", serviceKey)
                .queryParam("numOfRows", 1000)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseTimeInfo[0])
                .queryParam("base_time", baseTimeInfo[1])
                .queryParam("nx", gridX)
                .queryParam("ny", gridY)
                .build(true)
                .toUri();

        log.info("[Service] 기상청 외부 API 호출 시작: baseDate={}, baseTime={}, nx={}, ny={}",
                baseTimeInfo[0], baseTimeInfo[1], gridX, gridY);

        return webClient.get()
                .uri(uri)
                .retrieve()
                // 4xx/5xx 에러 발생 시 BusinessException(8003)으로 변환
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new BusinessException(WeatherErrorCode.WEATHER_API_ERROR)))
                .bodyToMono(WeatherApiResponseDto.class)
                .doOnSuccess(response -> log.info("[Service] 기상청 외부 API 호출 성공"))
                .doOnError(e -> log.error("[Service] 기상청 외부 API 호출 실패: {}", e.getMessage()));
    }

    /**
     * 기상청 단기예보 발표 스케줄에 맞춰 가장 최신 데이터를 가져올 수 있는 시간을 계산
     */
    private String[] calculateBaseTime() {
        LocalDateTime target = LocalDateTime.now(KST).minusMinutes(20);
        String baseDate = target.format(DATE_FORMATTER);
        int hour = target.getHour();

        String baseTime;
        if (hour < 2) {
            baseDate = target.minusDays(1).format(DATE_FORMATTER);
            baseTime = "2300";
        } else if (hour < 5) baseTime = "0200";
        else if (hour < 8) baseTime = "0500";
        else if (hour < 11) baseTime = "0800";
        else if (hour < 14) baseTime = "1100";
        else if (hour < 17) baseTime = "1400";
        else if (hour < 20) baseTime = "1700";
        else if (hour < 23) baseTime = "2000";
        else baseTime = "2300";

        log.debug("[Service] 계산된 기상청 발표 시간: baseDate={}, baseTime={}", baseDate, baseTime);

        return new String[]{baseDate, baseTime};
    }
}