package com.gitggal.clothesplz.exception.code;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

  USER_NOT_FOUND(1001, HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
  DUPLICATE_EMAIL(1002, HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
  INVALID_PASSWORD(1003, HttpStatus.BAD_REQUEST, "비밀번호가 올바르지 않습니다.");

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
