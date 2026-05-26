package com.gitggal.clothesplz.security.oauth;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import java.util.Map;

public class OAuthUserInfoFactory {

  public static OAuthUserInformation getOAuth2UserInfo(
      String registrationId,
      Map<String, Object> attributes
  ) {

    String normalized = (registrationId == null ? "" : registrationId.trim());
    if (normalized.isEmpty()) {
      throw new BusinessException(UserErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
    }

    return switch (registrationId.toUpperCase()) {
      case "KAKAO" -> new KakaoOAuthUserInformation(attributes);
      case "GOOGLE" -> new GoogleOAuthUserInformation(attributes);
      default -> throw new BusinessException(UserErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
    };
  }
}