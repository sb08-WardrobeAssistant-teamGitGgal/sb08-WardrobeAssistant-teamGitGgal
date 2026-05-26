package com.gitggal.clothesplz.security.oauth;

import com.gitggal.clothesplz.entity.user.SocialProvider;
import java.util.Map;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class GoogleOAuthUserInformation implements OAuthUserInformation {

  private final Map<String, Object> attributes;

  @Override
  public OAuthInformation toOAuthInformation() {

    String providerId = (String) attributes.get("sub");
    String email = (String) attributes.get("email");
    String nickname = (String) attributes.get("name");

    if (email == null || email.isBlank()) {
      email = "google_" + providerId + "@temp.clothesplz.com";
    }

    if (nickname == null || nickname.isBlank()) {
      nickname = "User" + shortRandom();
    }

    return new OAuthInformation(
        SocialProvider.GOOGLE,
        providerId,
        email,
        nickname
    );
  }

  private String shortRandom() {
    return String.format("%05d", new Random().nextInt(100000));
  }
}