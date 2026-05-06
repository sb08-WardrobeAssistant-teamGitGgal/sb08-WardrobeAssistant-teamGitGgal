package com.gitggal.clothesplz.exception.image;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ErrorCode;

public class ImageInvalidException extends BusinessException {

  @Override
  public ErrorCode getErrorCode() {
    return super.getErrorCode();
  }

  public ImageInvalidException(ErrorCode errorCode) {
    super(errorCode);
  }
}
