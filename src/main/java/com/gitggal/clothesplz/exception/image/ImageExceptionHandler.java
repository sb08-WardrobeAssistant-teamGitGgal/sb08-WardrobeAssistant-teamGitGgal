package com.gitggal.clothesplz.exception.image;

import com.gitggal.clothesplz.exception.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ImageExceptionHandler {

  @ExceptionHandler(ImageUploadFailedException.class)
  public ResponseEntity<ErrorResponse> handleImageUploadException(ImageUploadFailedException e) {

    log.error("이미지 업로드 중 오류가 발생하였습니다.", e);

    return ResponseEntity
        .status(e.getErrorCode().getHttpStatus())
        .body(ErrorResponse.of(e.getErrorCode()));
  }

  @ExceptionHandler(ImageInvalidException.class)
  public ResponseEntity<ErrorResponse> handleImageInvalidException(ImageInvalidException e) {

    log.error("이미지에 오류가 있습니다.", e);

    return ResponseEntity
        .status(e.getErrorCode().getHttpStatus())
        .body(ErrorResponse.of(e.getErrorCode()));
  }
}
