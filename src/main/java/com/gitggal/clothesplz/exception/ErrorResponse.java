package com.gitggal.clothesplz.exception;

import com.gitggal.clothesplz.exception.code.ErrorCode;
import java.util.Map;

public record ErrorResponse(String exceptionName, String message, Map<String, String> details) {

  public static ErrorResponse of(ErrorCode errorCode) {
    return new ErrorResponse(errorCode.name(), errorCode.getMessage(), null);
  }

  public static ErrorResponse of(ErrorCode errorCode, Map<String, String> details) {
    return new ErrorResponse(errorCode.name(), errorCode.getMessage(), details);
  }
}
