package com.gitggal.clothesplz.dto.profile.request;

import com.gitggal.clothesplz.dto.profile.common.WeatherAPILocation;
import com.gitggal.clothesplz.entity.profile.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ProfileUpdateRequest(
    @Size(max = 20)
    String name,

    Gender gender,

    LocalDate birthDate,

    @Valid
    WeatherAPILocation location,

    @Min(1)
    @Max(5)
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
