package com.gitggal.clothesplz.dto.weather;

import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.SkyStatus;

import java.time.LocalDate;

// 하루 동안의 날씨 예보 정보를 담는 DTO
public record DailyWeatherForecastDto(
        LocalDate date,
        SkyStatus skyStatus,
        Double avgTemp,
        Double minTemp,
        Double maxTemp,
        Double humidityCurrent,
        Double humidityComparedToDayBefore,
        PrecipitationType precipitationType,
        Double precipitationAmount,
        Double precipitationProbability,
        Double windSpeed,
        Double temperatureComparedToDayBefore
) {}

