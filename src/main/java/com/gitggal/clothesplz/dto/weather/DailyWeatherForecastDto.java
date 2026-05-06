package com.gitggal.clothesplz.dto.weather;

import com.gitggal.clothesplz.entity.weather.SkyStatus;
import lombok.*;
import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DailyWeatherForecastDto {

    private LocalDate date;        // 날짜 (오늘, 내일 등)

    private SkyStatus skyStatus;   // 날씨: 구름많음 (아이콘 및 텍스트용)

    private Double avgTemp;        // 평균: 18°

    private Double minTemp;        // 최저: 14°

    private Double maxTemp;        // 최고: 24°

}