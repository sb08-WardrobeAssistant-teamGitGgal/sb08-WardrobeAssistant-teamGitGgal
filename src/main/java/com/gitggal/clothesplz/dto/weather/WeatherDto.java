package com.gitggal.clothesplz.dto.weather;

import com.gitggal.clothesplz.entity.weather.SkyStatus;
import com.gitggal.clothesplz.entity.weather.WindPhrase;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WeatherDto {

    private UUID id;
    private LocalDateTime forecastedAt;
    private LocalDateTime forecastAt;
    private WeatherAPILocationDto location;
    private SkyStatus skyStatus;
    private PrecipitationDto precipitation;
    private HumidityDto humidity;
    private TemperatureDto temperature;
    private WindSpeedDto windSpeed;

}
