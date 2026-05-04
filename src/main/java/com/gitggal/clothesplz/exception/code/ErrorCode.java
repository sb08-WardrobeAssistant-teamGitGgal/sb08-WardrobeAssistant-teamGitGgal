package com.gitggal.clothesplz.exception.code;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

  String name();

  int getCode();

  HttpStatus getHttpStatus();

  String getMessage();
}
