package com.gitggal.clothesplz.dto.weather;

import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import lombok.Builder;

@Builder
public record PrecipitationDto(
        PrecipitationType type,
        Double amount,
        Double probability
) {}

