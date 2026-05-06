package com.gitggal.clothesplz.exception.profile;

import com.gitggal.clothesplz.exception.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ProfileExceptionHandler {

  @ExceptionHandler(ProfileNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleProfileNotFoundException(ProfileNotFoundException e) {

    log.error("프로필을 찾을 수 없습니다.", e);

    return ResponseEntity
        .status(e.getErrorCode().getHttpStatus())
        .body(ErrorResponse.of(e.getErrorCode()));
  }
}
