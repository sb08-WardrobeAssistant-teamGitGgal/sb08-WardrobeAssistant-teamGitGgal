package com.gitggal.clothesplz.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitggal.clothesplz.entity.user.SocialProvider;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GoogleOAuthUserInformationTest {

  @Test
  @DisplayName("성공")
  void success_all_fields_present() {

    // given
    Map<String, Object> attributes = Map.of(
        "sub", "google-123",
        "email", "test@gmail.com",
        "name", "test"
    );

    GoogleOAuthUserInformation info = new GoogleOAuthUserInformation(attributes);

    // when
    OAuthInformation result = info.toOAuthInformation();

    // then
    assertThat(result.provider()).isEqualTo(SocialProvider.GOOGLE);
    assertThat(result.providerId()).isEqualTo("google-123");
    assertThat(result.email()).isEqualTo("test@gmail.com");
    assertThat(result.nickname()).isEqualTo("test");
  }

  @Test
  @DisplayName("이메일이 없을 경우")
  void email_blank_fallback() {

    // given
    Map<String, Object> attributes = Map.of(
        "sub", "google-123",
        "email", "",
        "name", "kim"
    );

    GoogleOAuthUserInformation info = new GoogleOAuthUserInformation(attributes);

    // when
    OAuthInformation result = info.toOAuthInformation();

    // then
    assertThat(result.email())
        .isEqualTo("google_google-123@temp.clothesplz.com");
  }

  @Test
  @DisplayName("닉네임이 없을 경우")
  void nickname_blank_fallback() {

    // given
    Map<String, Object> attributes = Map.of(
        "sub", "google-123",
        "email", "test@gmail.com",
        "name", ""
    );

    GoogleOAuthUserInformation info = new GoogleOAuthUserInformation(attributes);

    // when
    OAuthInformation result = info.toOAuthInformation();

    // then
    assertThat(result.nickname()).startsWith("User");
    assertThat(result.nickname().length()).isGreaterThan(4);
  }
}