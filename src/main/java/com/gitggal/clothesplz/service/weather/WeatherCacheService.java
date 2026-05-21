package com.gitggal.clothesplz.service.weather;

import com.gitggal.clothesplz.dto.weather.WeatherDto;

import java.util.List;
import java.util.Optional;

public interface WeatherCacheService {

    Optional<List<WeatherDto>> getForecast(int nx, int ny);

    void saveForecast(int nx, int ny, List<WeatherDto> data);

    void evictForecast(int nx, int ny);
}
