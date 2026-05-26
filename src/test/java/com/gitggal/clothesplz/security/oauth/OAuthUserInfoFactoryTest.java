package com.gitggal.clothesplz.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuthUserInfoFactoryTest {

  @Test
  @DisplayName("GOOGLE")
  void google_success() {
    OAuthUserInformation result = OAuthUserInfoFactory.getOAuth2UserInfo("google", Map.of());

    assertThat(result).isInstanceOf(GoogleOAuthUserInformation.class);
  }

  @Test
  @DisplayName("KAKAO")
  void kakao_success() {
    OAuthUserInformation result = OAuthUserInfoFactory.getOAuth2UserInfo("kakao", Map.of());

    assertThat(result).isInstanceOf(KakaoOAuthUserInformation.class);
  }

  @Test
  @DisplayName("지원하지 않는 provider면 예외 발생")
  void unsupported_provider_fail() {
    assertThatThrownBy(() ->
        OAuthUserInfoFactory.getOAuth2UserInfo("naver", Map.of())
    )
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
  }

  @Test
  @DisplayName("빈 문자열 provider면 예외 발생")
  void blank_provider_fail() {

    assertThatThrownBy(() ->
        OAuthUserInfoFactory.getOAuth2UserInfo("   ", Map.of())
    ).isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.UNSUPPORTED_OAUTH_PROVIDER);

  }
}