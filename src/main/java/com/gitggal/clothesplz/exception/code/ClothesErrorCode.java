package com.gitggal.clothesplz.exception.code;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum ClothesErrorCode implements ErrorCode {

  CLOTHES_NOT_FOUND(7001, HttpStatus.NOT_FOUND, "의류를 찾을 수 없습니다."),
  DUPLICATE_CLOTHES_ATTRIBUTE_DEFINITION_ID(7002, HttpStatus.BAD_REQUEST, "중복된 의상 속성 정의 ID가 포함되었습니다."),
  CLOTHES_ATTRIBUTE_DEFINITION_NOT_FOUND(7003, HttpStatus.BAD_REQUEST, "존재하지 않는 의상 속성 정의 ID가 포함되었습니다."),
  INVALID_CLOTHES_ATTRIBUTE_VALUE(7004, HttpStatus.BAD_REQUEST, "허용되지 않는 의상 속성 값입니다.");

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
