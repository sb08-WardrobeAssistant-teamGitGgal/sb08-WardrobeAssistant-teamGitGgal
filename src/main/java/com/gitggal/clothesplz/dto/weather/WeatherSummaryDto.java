package com.gitggal.clothesplz.dto.weather;

import com.gitggal.clothesplz.entity.weather.SkyStatus;
import lombok.Builder;

import java.util.UUID;

@Builder
public record WeatherSummaryDto(
        UUID weatherId,
        SkyStatus skyStatus,
        PrecipitationDto precipitation,
        TemperatureDto temperature
) {}

