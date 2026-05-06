package com.gitggal.clothesplz.dto.profile.common;

import java.util.List;

public record WeatherAPILocation(
    Double latitude,
    Double longitude,
    Integer x,
    Integer y,
    List<String> locationNames
) {

  public static WeatherAPILocation of(
      Double latitude,
      Double longitude,
      Integer x,
      Integer y,
      List<String> locationNames
  ) {
    return new WeatherAPILocation(
        latitude,
        longitude,
        x,
        y,
        locationNames
    );
  }
}
