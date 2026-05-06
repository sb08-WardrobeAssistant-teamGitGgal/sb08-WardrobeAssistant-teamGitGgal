package com.gitggal.clothesplz.exception.image;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ErrorCode;

public class ImageUploadFailedException extends BusinessException {

  @Override
  public ErrorCode getErrorCode() {
    return super.getErrorCode();
  }

  public ImageUploadFailedException(ErrorCode errorCode) {
    super(errorCode);
  }
}
