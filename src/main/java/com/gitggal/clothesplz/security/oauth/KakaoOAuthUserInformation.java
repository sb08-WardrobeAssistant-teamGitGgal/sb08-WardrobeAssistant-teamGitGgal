package com.gitggal.clothesplz.security.oauth;

import com.gitggal.clothesplz.entity.user.SocialProvider;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import java.util.Map;
import java.util.Random;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KakaoOAuthUserInformation implements OAuthUserInformation {

  private final Map<String, Object> attributes;

  @Override
  public OAuthInformation toOAuthInformation() {

    Object id = attributes.get("id");
    if (id == null) {
      throw new BusinessException(UserErrorCode.INVALID_OAUTH_PROVIDER_ID);
    }
    String providerId = String.valueOf(id);
    Map<String, Object> kakaoAccount = getMap(attributes, "kakao_account");
    String email = (String) kakaoAccount.get("email");
    Map<String, Object> profile = getMap(kakaoAccount, "profile");
    String nickname = (String) profile.get("nickname");

    if (nickname == null || nickname.isBlank()) {
      nickname = "User" + shortRandom();
    }

    if (email == null || email.isBlank()) {
      email = nickname + "_" + providerId + "@kakao.com";
    }

    return new OAuthInformation(
        SocialProvider.KAKAO,
        providerId,
        email,
        nickname
    );
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getMap(Map<String, Object> source, String key) {
    Object value = source.get(key);

    if (value instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }

    return Map.of();
  }

  private String shortRandom() {
    return String.format("%05d", new Random().nextInt(100000));
  }
}