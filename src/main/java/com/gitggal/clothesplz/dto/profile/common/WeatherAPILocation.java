package com.gitggal.clothesplz.dto.profile.common;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record WeatherAPILocation(
    @NotNull(message = "위도는 필수입니다.")
    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
    @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
    Double latitude,

    @NotNull(message = "경도는 필수입니다.")
    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
    @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
    Double longitude,

    @NotNull(message = "격자 X 값은 필수입니다.")
    @Min(value = 0, message = "격자 X 값은 0 이상이어야 합니다.")
    @Max(value = 149, message = "격자 X 값은 149 이하여야 합니다.")
    Integer x,

    @NotNull(message = "격자 Y 값은 필수입니다.")
    @Min(value = 0, message = "격자 Y 값은 0 이상이어야 합니다.")
    @Max(value = 149, message = "격자 Y 값은 149 이하여야 합니다.")
    Integer y,

    @NotNull(message = "위치 이름 목록은 필수입니다.")
    @Size(min = 1, max = 10, message = "위치 이름 목록은 1개 이상 10개 이하여야 합니다.")
    List<@NotBlank(message = "위치 이름은 공백일 수 없습니다.") String> locationNames
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
