package com.gitggal.clothesplz.exception.user;

import com.gitggal.clothesplz.exception.code.UserErrorCode;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class DuplicateEmailException extends UserException {

  private Map<String, String> details = new HashMap<>();

  public DuplicateEmailException() {
    super(UserErrorCode.DUPLICATE_EMAIL);
  }

  public DuplicateEmailException(String email) {
    super(UserErrorCode.DUPLICATE_EMAIL);
    this.details.put("email", email);
  }
}
