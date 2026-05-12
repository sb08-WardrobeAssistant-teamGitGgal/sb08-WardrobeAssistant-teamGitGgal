package com.gitggal.clothesplz.exception.code;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

  // 사용자
  USER_NOT_FOUND(1001, HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
  DUPLICATE_EMAIL(1002, HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
  INVALID_PASSWORD(1003, HttpStatus.BAD_REQUEST, "비밀번호가 올바르지 않습니다."),

  // 인증
  AUTHENTICATION_FAILED(1101, HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
  AUTHENTICATION_PRINCIPAL_INVALID(1102, HttpStatus.INTERNAL_SERVER_ERROR, "인증 사용자 정보가 올바르지 않습니다."),
  INVALID_TOKEN(1103, HttpStatus.UNAUTHORIZED, "토큰이 올바르지 않습니다."),
  EXPIRED_TEMP_PASSWORD(1104,HttpStatus.UNAUTHORIZED,"임시 비밀번호가 만료되었습니다."),

  // JWT
  JWT_TOKEN_INVALID(1201, HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않습니다."),
  JWT_TOKEN_EXPIRED(1202, HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
  JWT_TOKEN_PARSE_FAILED(1203, HttpStatus.UNAUTHORIZED, "토큰 파싱에 실패했습니다."),
  JWT_TOKEN_GENERATION_FAILED(1204, HttpStatus.INTERNAL_SERVER_ERROR, "토큰 생성에 실패했습니다."),
  JWT_TOKEN_NOT_FOUND(1205, HttpStatus.UNAUTHORIZED, "토큰을 찾을 수 없습니다.");


  private final int code;
  private final HttpStatus httpStatus;
  private final String message;

  @Override
  public int getCode() {
    return code;
  }

  @Override
  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
