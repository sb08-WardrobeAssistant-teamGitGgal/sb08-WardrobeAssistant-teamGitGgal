package com.gitggal.clothesplz.exception.code;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum MessageErrorCode implements ErrorCode {

  MESSAGE_NOT_FOUND(4001, HttpStatus.NOT_FOUND, "메시지를 찾을 수 없습니다."),
  UNAUTHORIZED_MESSAGE_ACCESS(4002, HttpStatus.FORBIDDEN, "해당 메시지에 접근 권한이 없습니다.");

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
