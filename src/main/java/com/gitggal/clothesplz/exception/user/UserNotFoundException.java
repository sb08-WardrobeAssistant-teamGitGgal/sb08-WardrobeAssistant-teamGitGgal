package com.gitggal.clothesplz.exception.user;

import com.gitggal.clothesplz.exception.code.UserErrorCode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;

@Getter
public class UserNotFoundException extends UserException {

  private Map<String, String> details = new HashMap<>();

  public UserNotFoundException() {
    super(UserErrorCode.USER_NOT_FOUND);
  }

  public UserNotFoundException(UUID userId) {
    super(UserErrorCode.USER_NOT_FOUND);
    this.details.put("userId", userId.toString());
  }

}
