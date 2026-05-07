package com.gitggal.clothesplz.exception.code;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum FollowErrorCode implements ErrorCode {

  INVALID_CURSOR_FORMAT(3001, HttpStatus.BAD_REQUEST, "cursor 형식이 올바르지 않습니다.");

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
