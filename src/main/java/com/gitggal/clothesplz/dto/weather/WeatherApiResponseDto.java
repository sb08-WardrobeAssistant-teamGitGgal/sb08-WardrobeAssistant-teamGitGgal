package com.gitggal.clothesplz.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 외부 날씨 API 응답을 매핑하기 위한 DTO
 * 필드 계층 구조: response > body > items > item(List)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WeatherApiResponseDto(Response response) {

    public record Response(Body body) {}

    public record Body(Items items) {}

    public record Items(List<WeatherItem> item) {}

    public record WeatherItem(
            String category, // 자료구분 코드
            String fcstDate, // 예측 일자
            String fcstTime, // 예측 시간
            String fcstValue // 예보 값
    ) {}
}