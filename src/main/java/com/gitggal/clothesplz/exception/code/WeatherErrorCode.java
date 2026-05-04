package com.gitggal.clothesplz.exception.code;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum WeatherErrorCode implements ErrorCode {

  WEATHER_NOT_FOUND(8001, HttpStatus.NOT_FOUND, "날씨 정보를 찾을 수 없습니다."),
  LOCATION_NOT_FOUND(8002, HttpStatus.NOT_FOUND, "위치 정보를 찾을 수 없습니다."),
  WEATHER_API_ERROR(8003, HttpStatus.INTERNAL_SERVER_ERROR, "날씨 API 호출 중 오류가 발생했습니다.");

  private final int code;
  private final HttpStatus httpStatus;
  private final String message;

  @Override
  public int getCode() { return code; }

  @Override
  public HttpStatus getHttpStatus() { return httpStatus; }

  @Override
  public String getMessage() { return message; }
}
