package com.gitggal.clothesplz.exception.code;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

  INVALID_CURSOR_FORMAT(5001, HttpStatus.BAD_REQUEST, "cursor 형식이 올바르지 않습니다."),
  NOTIFICATION_NOT_FOUND(5002, HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
  UNAUTHORIZED_NOTIFICATION_ACCESS(5003, HttpStatus.FORBIDDEN, "본인의 알림만 접근할 수 있습니다.");

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
