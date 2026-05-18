package com.gitggal.clothesplz.util;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.CommonErrorCode;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import java.util.UUID;

public class AuthenticationUtil {

  private AuthenticationUtil() {}

  public static UUID extractUserId(ClothesUserDetails userDetails) {
    if (userDetails == null || userDetails.getUserDto() == null) {
      throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
    }
    return userDetails.getUserDto().id();
  }
}
