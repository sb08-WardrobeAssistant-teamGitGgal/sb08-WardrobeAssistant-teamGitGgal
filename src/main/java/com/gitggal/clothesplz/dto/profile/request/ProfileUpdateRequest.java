package com.gitggal.clothesplz.dto.profile.request;

import com.gitggal.clothesplz.dto.profile.common.WeatherAPILocation;
import com.gitggal.clothesplz.entity.profile.Gender;
import java.time.LocalDate;

public record ProfileUpdateRequest(
    String name,
    Gender gender,
    LocalDate birthDate,
    WeatherAPILocation location,
    Integer temperatureSensitivity
) {

  public static ProfileUpdateRequest of(
      String name,
      Gender gender,
      LocalDate birthDate,
      WeatherAPILocation location,
      Integer temperatureSensitivity
  ) {
    return new ProfileUpdateRequest(
        name,
        gender,
        birthDate,
        location,
        temperatureSensitivity
    );
  }
}
