package com.gitggal.clothesplz.service.weather;

import com.gitggal.clothesplz.dto.weather.DailyWeatherForecastDto;
import com.gitggal.clothesplz.dto.weather.WeatherApiResponseDto;

import java.util.List;

public interface WeatherParserService {

    List<DailyWeatherForecastDto> parseDailyForecast(WeatherApiResponseDto response);
}
