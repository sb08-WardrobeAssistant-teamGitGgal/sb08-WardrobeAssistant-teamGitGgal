package com.gitggal.clothesplz.exception.code;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

  INVALID_INPUT(1, HttpStatus.BAD_REQUEST, "잘못된 입력입니다."),
  UNAUTHORIZED(2, HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
  FORBIDDEN(3, HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
  NOT_FOUND(4, HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
  INTERNAL_SERVER_ERROR(5, HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

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
