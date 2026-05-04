package com.gitggal.clothesplz.repository.weather;

import com.gitggal.clothesplz.entity.weather.Weather;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WeatherRepository extends JpaRepository<Weather, UUID> {
}
