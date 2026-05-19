package com.gitggal.clothesplz.mapper.weather;

import com.gitggal.clothesplz.dto.weather.HumidityDto;
import com.gitggal.clothesplz.dto.weather.PrecipitationDto;
import com.gitggal.clothesplz.dto.weather.TemperatureDto;
import com.gitggal.clothesplz.dto.weather.WeatherAPILocationDto;
import com.gitggal.clothesplz.dto.weather.WeatherDto;
import com.gitggal.clothesplz.dto.weather.WindSpeedDto;
import com.gitggal.clothesplz.entity.weather.Weather;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WeatherMapper {

    // DB 엔티티 리스트를 DTO 리스트로 변환 (DB 조회/저장은 서비스에서 처리 후 전달)
    public List<WeatherDto> toWeatherDtoList(
            List<Weather> weathers,
            double latitude, double longitude,
            int gridX, int gridY,
            List<String> locationNames) {
        return weathers.stream()
                .map(w -> toWeatherDto(w, latitude, longitude, gridX, gridY, locationNames))
                .toList();
    }

    // Weather 엔티티의 실제 DB UUID를 사용해 DTO 생성 (기존 stableWeatherId 대체)
    public WeatherDto toWeatherDto(
            Weather weather,
            double latitude, double longitude,
            int gridX, int gridY,
            List<String> locationNames) {
        WeatherAPILocationDto location = new WeatherAPILocationDto(latitude, longitude, gridX, gridY, locationNames);
        PrecipitationDto precipitation = new PrecipitationDto(
                weather.getPrecipitationType(),
                weather.getPrecipitationAmount(),
                weather.getPrecipitationProbability());
        HumidityDto humidity = new HumidityDto(weather.getHumidity(), weather.getHumidityDiff());
        TemperatureDto temperature = new TemperatureDto(
                weather.getTemperatureCurrent(),
                weather.getTemperatureDiff(),
                weather.getTemperatureMin(),
                weather.getTemperatureMax());
        WindSpeedDto windSpeed = new WindSpeedDto(weather.getWindSpeed(), weather.getWindPhrase());

        return new WeatherDto(
                weather.getId(),
                weather.getForecastedAt().toLocalDateTime(),
                weather.getForecastAt().toLocalDateTime(),
                location,
                weather.getSkyStatus(),
                precipitation,
                humidity,
                temperature,
                windSpeed);
    }

    public WeatherAPILocationDto toLocationDto(
            double latitude, double longitude,
            int gridX, int gridY,
            List<String> locationNames) {
        return new WeatherAPILocationDto(latitude, longitude, gridX, gridY, locationNames);
    }
}
