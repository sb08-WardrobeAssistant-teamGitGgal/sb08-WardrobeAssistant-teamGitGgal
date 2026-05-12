package com.gitggal.clothesplz.service.weather;

import com.gitggal.clothesplz.dto.weather.WeatherApiResponseDto;
import reactor.core.publisher.Mono;

public interface WeatherApiService {

    Mono<WeatherApiResponseDto> fetchWeather(Integer gridX, Integer gridY);
}
