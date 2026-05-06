package com.gitggal.clothesplz.dto.weather;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WeatherAPILocationDto {

    private Double latitude;
    private Double longitude;
    private Integer x;
    private Integer y;
    private List<String> locationNames;
}

