package com.gitggal.clothesplz.dto.weather;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class WeatherApiResponseDto {
    private Response response;

    @Getter
    @NoArgsConstructor
    public static class Response {
        private Body body;
    }

    @Getter
    @NoArgsConstructor
    public static class Body {
        private Items items;
    }

    @Getter
    @NoArgsConstructor
    public static class Items {
        private List<WeatherItem> item;
    }

    @Getter
    @NoArgsConstructor
    public static class WeatherItem {
        private String category;  // TMP(기온), REH(습도), SKY(하늘상태) 등
        private String fcstDate;  // 예보 일자
        private String fcstTime;  // 예보 시각
        private String fcstValue; // 예보 값
    }

}
