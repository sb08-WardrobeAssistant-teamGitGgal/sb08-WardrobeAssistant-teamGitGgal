package com.gitggal.clothesplz.exception.user;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ErrorCode;

public class UserException extends BusinessException {

  public UserException(ErrorCode errorCode) {
    super(errorCode);
  }

}
