package com.gitggal.clothesplz.exception;

import com.gitggal.clothesplz.exception.code.CommonErrorCode;
import com.gitggal.clothesplz.exception.code.FeedErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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

  // 락 획득 실패나 타임아웃 발생했을 때 예외 처리하는 핸들러
  @ExceptionHandler(PessimisticLockingFailureException.class)
  public ResponseEntity<ErrorResponse> handlePessimisticLockingFailure(PessimisticLockingFailureException e) {
    log.warn("Lock acquisition failed: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of(FeedErrorCode.LOCK_ACQUISITION_FAILED));
  }

  // 권한 없는 접근시 발생 -> 403
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
    log.warn("Access denied: {}", e.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ErrorResponse.of(CommonErrorCode.FORBIDDEN));
  }


  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e) {
    log.error("Unhandled exception", e);
    return ResponseEntity.internalServerError()
        .body(ErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR));
  }
}
