package com.gitggal.clothesplz.dto.profile.response;

import com.gitggal.clothesplz.dto.profile.common.WeatherAPILocation;
import com.gitggal.clothesplz.entity.profile.Gender;
import java.time.LocalDate;
import java.util.UUID;

public record ProfileDto(
    UUID userId,
    String name,
    Gender gender,
    LocalDate birthDate,
    WeatherAPILocation location,
    Integer temperatureSensitivity,
    String profileImageUrl
) {
  public static ProfileDto of(
      UUID userId,
      String name,
      Gender gender,
      LocalDate birthDate,
      WeatherAPILocation location,
      Integer temperatureSensitivity,
      String profileImageUrl
  ) {
    return new ProfileDto(
        userId, name, gender, birthDate, location, temperatureSensitivity, profileImageUrl);
  }
}
