package com.gitggal.clothesplz.service.weather;

import com.gitggal.clothesplz.entity.weather.Weather;

public interface WeatherAlertService {

    void sendAlertsIfNeeded(Weather weather);
}

