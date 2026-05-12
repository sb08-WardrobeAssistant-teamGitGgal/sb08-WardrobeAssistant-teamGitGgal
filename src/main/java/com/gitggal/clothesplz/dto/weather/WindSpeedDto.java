package com.gitggal.clothesplz.dto.weather;

import com.gitggal.clothesplz.entity.weather.WindPhrase;

public record WindSpeedDto(
        Double speed,
        WindPhrase asWord
) {}

