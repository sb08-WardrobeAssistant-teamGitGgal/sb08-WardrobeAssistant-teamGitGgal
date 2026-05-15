package com.gitggal.clothesplz.repository.weather;

import com.gitggal.clothesplz.entity.weather.Location;
import com.gitggal.clothesplz.entity.weather.Weather;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeatherRepository extends JpaRepository<Weather, UUID> {

    Optional<Weather> findByLocationAndForecastAt(Location location, OffsetDateTime forecastAt);

    // 배치가 저장할 날짜 범위만 조회.
    List<Weather> findByLocationInAndForecastAtBetween(List<Location> locations, OffsetDateTime start, OffsetDateTime end);

    Optional<Weather> findFirstByLocationOrderByForecastAtDesc(Location location);

    // 특정 위치의 일정 기간(예: 오늘 하루) 예보 목록 가져오기
    List<Weather> findByLocationAndForecastAtBetween(
            Location location,
            OffsetDateTime start,
            OffsetDateTime end
    );
}