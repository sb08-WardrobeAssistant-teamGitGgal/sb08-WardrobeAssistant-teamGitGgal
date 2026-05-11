package com.gitggal.clothesplz.dto.weather;

import java.util.List;

public record WeatherAPILocationDto(
        Double latitude,
        Double longitude,
        Integer x,
        Integer y,
        List<String> locationNames
) {}

