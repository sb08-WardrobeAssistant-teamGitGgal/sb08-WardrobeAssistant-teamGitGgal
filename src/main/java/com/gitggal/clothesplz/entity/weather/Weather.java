package com.gitggal.clothesplz.entity.weather;

import com.gitggal.clothesplz.entity.base.BaseUpdatableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "weathers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Weather extends BaseUpdatableEntity {

    // 예보 발표 시각 (TIMESTAMPTZ)
    @Column(name = "forecasted_at", nullable = false)
    private OffsetDateTime forecastedAt;

    // 예보 대상 시각 (TIMESTAMPTZ)
    @Column(name = "forecast_at", nullable = false)
    private OffsetDateTime forecastAt;

    // 위치 ID (FK) - Location 엔티티가 구현되어 있다고 가정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    // 하늘 상태 (ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "sky_status", nullable = false, length = 20)
    private SkyStatus skyStatus;

    // 강수 유형 (ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "precipitation_type", nullable = false, length = 20)
    private PrecipitationType precipitationType;

    // 강수량
    @Column(name = "precipitation_amount", nullable = false)
    private Double precipitationAmount;

    // 강수 확률
    @Column(name = "precipitation_probability", nullable = false)
    private Double precipitationProbability;

    // 습도
    @Column(name = "humidity", nullable = false)
    private Double humidity;

    // 전일 습도 차 (humidity_diff)
    @Column(name = "humidity_diff", nullable = false)
    private Double humidityDiff;

    // 현재 기온 (temperature_current)
    @Column(name = "temperature_current", nullable = false)
    private Double temperatureCurrent;

    // 전일 온도 차 (temperature_diff)
    @Column(name = "temperature_diff", nullable = false)
    private Double temperatureDiff;

    // 최저 기온
    @Column(name = "temperature_min", nullable = false)
    private Double temperatureMin;

    // 최고 기온
    @Column(name = "temperature_max", nullable = false)
    private Double temperatureMax;

    // 풍속
    @Column(name = "wind_speed", nullable = false)
    private Double windSpeed;

    // 풍속 문구 (ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "wind_phrase", nullable = false, length = 20)
    private WindPhrase windPhrase;
}
