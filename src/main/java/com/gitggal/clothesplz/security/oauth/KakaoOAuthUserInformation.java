package com.gitggal.clothesplz.security.oauth;

import com.gitggal.clothesplz.entity.user.SocialProvider;
import java.util.Map;
import java.util.Random;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KakaoOAuthUserInformation implements OAuthUserInformation {

  private final Map<String, Object> attributes;

  @Override
  public OAuthInformation toOAuthInformation() {

    String providerId = String.valueOf(attributes.get("id"));
    Map<String, Object> kakaoAccount = getMap(attributes, "kakao_account");
    String email = (String) kakaoAccount.get("email");
    Map<String, Object> profile = getMap(kakaoAccount, "profile");
    String nickname = (String) profile.get("nickname");

    if (email == null || email.isBlank()) {
      email = nickname + "_" + providerId + "@kakao.com";
    }

    if (nickname == null || nickname.isBlank()) {
      nickname = "User" + shortRandom();
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