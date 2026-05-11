package com.gitggal.clothesplz.dto.weather;

import com.gitggal.clothesplz.entity.weather.SkyStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record WeatherDto(
        UUID id,
        LocalDateTime forecastedAt,
        LocalDateTime forecastAt,
        WeatherAPILocationDto location,
        SkyStatus skyStatus,
        PrecipitationDto precipitation,
        HumidityDto humidity,
        TemperatureDto temperature,
        WindSpeedDto windSpeed
) {}

