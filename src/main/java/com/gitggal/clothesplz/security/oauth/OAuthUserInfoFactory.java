package com.gitggal.clothesplz.security.oauth;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import java.util.Map;

public class OAuthUserInfoFactory {

  public static OAuthUserInformation getOAuth2UserInfo(
      String registrationId,
      Map<String, Object> attributes
  ) {

    return switch (registrationId.toUpperCase()) {
      case "KAKAO" -> new KakaoOAuthUserInformation(attributes);

      default -> throw new BusinessException(UserErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
    };
  }
}