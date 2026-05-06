package com.gitggal.clothesplz.exception.user;

import com.gitggal.clothesplz.exception.code.UserErrorCode;
import java.util.HashMap;
import java.util.Map;

public class InvalidPasswordException extends UserException {

  private Map<String, String> details = new HashMap<>();

  public InvalidPasswordException() {
    super(UserErrorCode.INVALID_PASSWORD);
  }

  public InvalidPasswordException(String password) {
    super(UserErrorCode.INVALID_PASSWORD);
    this.details.put("invalid password", "password");
  }
}
