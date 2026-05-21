package com.gitggal.clothesplz.service.weather;

import com.gitggal.clothesplz.dto.weather.DailyWeatherForecastDto;
import com.gitggal.clothesplz.entity.weather.Location;
import com.gitggal.clothesplz.entity.weather.Weather;

import java.time.OffsetDateTime;

public interface WeatherPersistenceService {

    Location findOrCreateLocation(double lat, double lon, int nx, int ny, String locationNamesStr);

    Location findLocationOrThrow(int nx, int ny);

    Weather findWeatherOrThrow(Location location, OffsetDateTime forecastAt);

    Weather findOrCreateWeather(Location location, DailyWeatherForecastDto dto);
}
