package com.gitggal.clothesplz.exception.code;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

  NOTIFICATION_NOT_FOUND(5001, HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
  UNAUTHORIZED_NOTIFICATION_ACCESS(5002, HttpStatus.FORBIDDEN, "해당 알림에 접근 권한이 없습니다.");

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
