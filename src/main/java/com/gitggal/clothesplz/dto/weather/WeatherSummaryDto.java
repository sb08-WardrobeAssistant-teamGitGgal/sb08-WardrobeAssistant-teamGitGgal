package com.gitggal.clothesplz.dto.weather;

import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.SkyStatus;
import lombok.*;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WeatherSummaryDto {

    private UUID weatherId;
    private SkyStatus skyStatus; // enum 사용
    private PrecipitationDto precipitation;
    private TemperatureDto temperature;
}
