package com.gitggal.clothesplz.exception.code;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum ProfileErrorCode implements ErrorCode {

  PROFILE_NOT_FOUND(6001, HttpStatus.NOT_FOUND, "프로필을 찾을 수 없습니다."),
  DUPLICATE_NICKNAME(6002, HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.");

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
