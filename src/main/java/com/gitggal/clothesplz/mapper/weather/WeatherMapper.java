package com.gitggal.clothesplz.mapper.weather;

import com.gitggal.clothesplz.dto.weather.DailyWeatherForecastDto;
import com.gitggal.clothesplz.dto.weather.HumidityDto;
import com.gitggal.clothesplz.dto.weather.PrecipitationDto;
import com.gitggal.clothesplz.dto.weather.TemperatureDto;
import com.gitggal.clothesplz.dto.weather.WeatherAPILocationDto;
import com.gitggal.clothesplz.dto.weather.WeatherDto;
import com.gitggal.clothesplz.dto.weather.WindSpeedDto;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.WindPhrase;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class WeatherMapper {
    private static final double WIND_MODERATE_THRESHOLD = 4.0; // m/s, 기상청 기준
    private static final double WIND_STRONG_THRESHOLD = 9.0;   // m/s, 기상청 기준

    // 일별 내부 DTO 목록을 API 계약용 WeatherDto 목록으로 변환한다.
    public List<WeatherDto> toWeatherDtoList(
            List<DailyWeatherForecastDto> forecasts,
            double latitude,
            double longitude,
            int gridX,
            int gridY) {
        return forecasts.stream()
                .map(dto -> toWeatherDto(dto, latitude, longitude, gridX, gridY))
                .toList();
    }

    // 하루 단위 요약 예보와 요청 위치 정보를 묶어 WeatherDto 한 건을 만든다.
    public WeatherDto toWeatherDto(
            DailyWeatherForecastDto dto,
            double latitude,
            double longitude,
            int gridX,
            int gridY) {
        UUID weatherId = stableWeatherId(dto, gridX, gridY);
        return WeatherDto.builder()
                .id(weatherId)
                .forecastedAt(LocalDateTime.now())
                .forecastAt(dto.getDate().atStartOfDay())
                .skyStatus(dto.getSkyStatus())
                .temperature(
                        TemperatureDto.builder()
                                .current(dto.getAvgTemp())
                                .min(dto.getMinTemp())
                                .max(dto.getMaxTemp())
                                .comparedToDayBefore(dto.getTemperatureComparedToDayBefore())
                                .build())
                .location(
                        WeatherAPILocationDto.builder()
                                .latitude(latitude)
                                .longitude(longitude)
                                .x(gridX)
                                .y(gridY)
                                .locationNames(List.of()) // 행정구역명은 카카오 등 연동 시 채움
                                .build())
                .precipitation(
                        PrecipitationDto.builder()
                                .type(dto.getPrecipitationType() == null ? PrecipitationType.NONE : dto.getPrecipitationType())
                                .amount(dto.getPrecipitationAmount())
                                .probability(dto.getPrecipitationProbability())
                                .build())
                .humidity(HumidityDto.builder()
                        .current(dto.getHumidityCurrent())
                        .comparedToDayBefore(dto.getHumidityComparedToDayBefore())
                        .build())
                .windSpeed(WindSpeedDto.builder()
                        .speed(dto.getWindSpeed())
                        .asWord(toWindPhrase(dto.getWindSpeed()))
                        .build())
                .build();
    }

    public WeatherAPILocationDto toLocationDto(double latitude, double longitude, int gridX, int gridY) {
        return WeatherAPILocationDto.builder()
                .latitude(latitude)
                .longitude(longitude)
                .x(gridX)
                .y(gridY)
                .locationNames(List.of())
                .build();
    }

    private WindPhrase toWindPhrase(Double speed) {
        if (speed == null || speed < WIND_MODERATE_THRESHOLD) {
            return WindPhrase.WEAK;
        }
        if (speed < WIND_STRONG_THRESHOLD) {
            return WindPhrase.MODERATE;
        }
        return WindPhrase.STRONG;
    }

    private UUID stableWeatherId(DailyWeatherForecastDto dto, int gridX, int gridY) {
        String identitySource = "%d:%d:%s".formatted(gridX, gridY, dto.getDate());
        return UUID.nameUUIDFromBytes(identitySource.getBytes(StandardCharsets.UTF_8));
    }
}
