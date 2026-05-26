package com.gitggal.clothesplz.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitggal.clothesplz.entity.user.SocialProvider;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KakaoOAuthUserInformationTest {

  @Test
  @DisplayName("성공")
  void success_all_fields_present() {

    // given
    Map<String, Object> profile = Map.of(
        "nickname", "test"
    );

    Map<String, Object> kakaoAccount = Map.of(
        "email", "test@kakao.com",
        "profile", profile
    );

    Map<String, Object> attributes = Map.of(
        "id", 12345,
        "kakao_account", kakaoAccount
    );

    KakaoOAuthUserInformation info = new KakaoOAuthUserInformation(attributes);

    // when
    OAuthInformation result = info.toOAuthInformation();

    // then
    assertThat(result.provider()).isEqualTo(SocialProvider.KAKAO);
    assertThat(result.providerId()).isEqualTo("12345");
    assertThat(result.email()).isEqualTo("test@kakao.com");
    assertThat(result.nickname()).isEqualTo("test");
  }

  @Test
  @DisplayName("email이 없을 경우")
  void email_null_fallback() {

    // given
    Map<String, Object> profile = Map.of(
        "nickname", "test"
    );

    Map<String, Object> kakaoAccount = Map.of(
        "profile", profile
    );

    Map<String, Object> attributes = Map.of(
        "id", 12345,
        "kakao_account", kakaoAccount
    );

    KakaoOAuthUserInformation info = new KakaoOAuthUserInformation(attributes);

    // when
    OAuthInformation result = info.toOAuthInformation();

    // then
    assertThat(result.email()).isEqualTo("test_12345@kakao.com");
  }

  @Test
  @DisplayName("nickname이 없을 경우")
  void nickname_null_fallback() {

    // given
    Map<String, Object> kakaoAccount = Map.of(
        "email", "test@kakao.com",
        "profile", Map.of()
    );

    Map<String, Object> attributes = Map.of(
        "id", 12345,
        "kakao_account", kakaoAccount
    );

    KakaoOAuthUserInformation info = new KakaoOAuthUserInformation(attributes);

    // when
    OAuthInformation result = info.toOAuthInformation();

    // then
    assertThat(result.nickname()).startsWith("User");
  }

  @Test
  @DisplayName("email, nickname 없을 경우")
  void both_missing_fallback() {

    // given
    Map<String, Object> kakaoAccount = Map.of(
        "profile", Map.of()
    );

    Map<String, Object> attributes = Map.of(
        "id", 12345,
        "kakao_account", kakaoAccount
    );

    KakaoOAuthUserInformation info = new KakaoOAuthUserInformation(attributes);

    // when
    OAuthInformation result = info.toOAuthInformation();

    // then
    assertThat(result.email()).contains("@kakao.com");
    assertThat(result.nickname()).startsWith("User");
  }
}