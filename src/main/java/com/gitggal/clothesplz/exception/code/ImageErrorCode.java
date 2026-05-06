package com.gitggal.clothesplz.exception.code;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum ImageErrorCode implements ErrorCode {

  IMAGE_EMPTY(9001, HttpStatus.BAD_REQUEST, "이미지 파일이 비어있습니다."),
  INVALID_IMAGE_CONTENT_TYPE(9002, HttpStatus.BAD_REQUEST, "이미지 파일만 업로드할 수 있습니다."),
  IMAGE_EXTENSION_NOT_FOUND(9003, HttpStatus.BAD_REQUEST, "이미지 확장자를 확인할 수 없습니다."),
  UNSUPPORTED_IMAGE_FORMAT(9004, HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다."),
  IMAGE_UPLOAD_FAILED(9005, HttpStatus.INTERNAL_SERVER_ERROR, "이미지 저장에 실패했습니다.");

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
