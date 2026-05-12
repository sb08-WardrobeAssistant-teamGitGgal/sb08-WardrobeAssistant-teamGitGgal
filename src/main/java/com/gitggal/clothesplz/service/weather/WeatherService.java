package com.gitggal.clothesplz.service.weather;

import com.gitggal.clothesplz.dto.weather.WeatherAPILocationDto;
import com.gitggal.clothesplz.dto.weather.WeatherDto;
import reactor.core.publisher.Mono;

import java.util.List;

public interface WeatherService {

    Mono<List<WeatherDto>> getWeatherForecast(double latitude, double longitude);

    Mono<WeatherAPILocationDto> getWeatherLocation(double latitude, double longitude);
}
