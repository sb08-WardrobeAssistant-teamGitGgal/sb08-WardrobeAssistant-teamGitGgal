package com.gitggal.clothesplz.service.weather.impl;

import com.gitggal.clothesplz.dto.weather.KakaoCoord2RegionResponseDto;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.WeatherErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoLocalApiServiceImpl {

    private final WebClient webClient;

    @Value("${kakao.api.rest-key}")
    private String restApiKey;

    private static final String KAKAO_COORD2REGION_URL = "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json";

    public Mono<List<String>> getLocationNames(double latitude, double longitude) {
        log.info("[Service] 카카오 좌표 → 행정구역 변환 시작: lat={}, lon={}", latitude, longitude);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("dapi.kakao.com")
                        .path("/v2/local/geo/coord2regioncode.json")
                        .queryParam("x", longitude)
                        .queryParam("y", latitude)
                        .build())
                .header("Authorization", "KakaoAK " + restApiKey)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new BusinessException(WeatherErrorCode.KAKAO_API_ERROR)))
                .bodyToMono(KakaoCoord2RegionResponseDto.class)
                .map(this::extractLocationNames)
                .doOnSuccess(names -> log.info("[Service] 카카오 행정구역 변환 성공: {}", names))
                .doOnError(e -> log.error("[Service] 카카오 행정구역 변환 실패: {}", e.getMessage()))
                .onErrorReturn(List.of());
    }

    private List<String> extractLocationNames(KakaoCoord2RegionResponseDto response) {
        return response.documents().stream()
                .filter(doc -> "H".equals(doc.regionType()))
                .findFirst()
                .map(doc -> List.of(
                        doc.region1DepthName(),
                        doc.region2DepthName(),
                        doc.region3DepthName()))
                .orElse(List.of());
    }
}
