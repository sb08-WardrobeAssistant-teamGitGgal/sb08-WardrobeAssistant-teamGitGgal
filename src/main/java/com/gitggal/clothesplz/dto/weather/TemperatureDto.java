package com.gitggal.clothesplz.dto.weather;

import lombok.Builder;

@Builder
public record TemperatureDto(
        double current,
        double comparedToDayBefore,
        double min,
        double max
) {}