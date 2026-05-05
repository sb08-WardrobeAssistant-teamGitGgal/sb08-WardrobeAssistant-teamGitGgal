package com.gitggal.clothesplz.exception.profile;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ErrorCode;

public class ProfileNotFoundException extends BusinessException {

  @Override
  public ErrorCode getErrorCode() {
    return super.getErrorCode();
  }

  public ProfileNotFoundException(ErrorCode errorCode) {
    super(errorCode);
  }
}
