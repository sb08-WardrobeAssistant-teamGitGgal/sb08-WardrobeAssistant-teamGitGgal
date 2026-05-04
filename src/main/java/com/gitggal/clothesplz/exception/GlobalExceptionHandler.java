package com.gitggal.clothesplz.exception;

import com.gitggal.clothesplz.exception.code.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
    var errorCode = e.getErrorCode();
    log.warn("code={}, message={}", errorCode.getCode(), e.getMessage());
    return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.of(errorCode));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
      MethodArgumentNotValidException e) {
    Map<String, String> details = e.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(
            fe -> fe.getField(),
            fe -> fe.getDefaultMessage(),
            (existing, duplicate) -> existing
        ));
    log.warn("Validation failed: {}", details);
    return ResponseEntity.badRequest()
        .body(ErrorResponse.of(CommonErrorCode.INVALID_INPUT, details));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
      MissingServletRequestParameterException e) {
    Map<String, String> details = Map.of(e.getParameterName(), "필수 파라미터가 누락되었습니다.");
    log.warn("Missing parameter: {}", details);
    return ResponseEntity.badRequest()
        .body(ErrorResponse.of(CommonErrorCode.INVALID_INPUT, details));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
      MethodArgumentTypeMismatchException e) {
    Map<String, String> details = Map.of(e.getName(), "올바르지 않은 타입입니다.");
    log.warn("Type mismatch: {}", details);
    return ResponseEntity.badRequest()
        .body(ErrorResponse.of(CommonErrorCode.INVALID_INPUT, details));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e) {
    log.error("Unhandled exception", e);
    return ResponseEntity.internalServerError()
        .body(ErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR));
  }
}
