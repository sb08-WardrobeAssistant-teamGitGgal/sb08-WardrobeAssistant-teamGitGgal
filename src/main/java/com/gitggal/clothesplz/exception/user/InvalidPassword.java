package com.gitggal.clothesplz.exception.user;

import com.gitggal.clothesplz.exception.code.UserErrorCode;
import java.util.HashMap;
import java.util.Map;

public class InvalidPassword extends UserException {

  private Map<String, String> details = new HashMap<>();

  public InvalidPassword() {
    super(UserErrorCode.DUPLICATE_EMAIL);
  }

  public InvalidPassword(String password) {
    super(UserErrorCode.DUPLICATE_EMAIL);
    this.details.put("password", password);
  }
}
