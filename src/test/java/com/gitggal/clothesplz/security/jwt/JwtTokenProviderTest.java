package com.gitggal.clothesplz.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.user.UserRole;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

class JwtTokenProviderTest {

  private JwtTokenProvider tokenProvider;
  private ClothesUserDetails userDetails;
  private UUID userId;

  @BeforeEach
  void setUp() throws Exception {
    JwtProperties jwtProperties = new JwtProperties(
        "abcdefghijklmnopqrstuvwxyz123456",
        900000,
        "zyxwvutsrqponmlkjihgfedcba654321",
        3600000
    );
    tokenProvider = new JwtTokenProvider(jwtProperties);

    userId = UUID.randomUUID();
    UserDto userDto = new UserDto(
        userId,
        Instant.now(),
        "test@test.com",
        "홍길동",
        UserRole.USER,
        false
    );
    userDetails = new ClothesUserDetails(userDto, "encoded-password");
  }

  @Test
  @DisplayName("Access Token, Refresh Token 생성")
  void generateAndValidateToken() throws Exception {
    String accessToken = tokenProvider.generateAccessToken(userDetails);
    String refreshToken = tokenProvider.generateRefreshToken(userDetails);

    assertThat(tokenProvider.validateAccessToken(accessToken)).isTrue();
    assertThat(tokenProvider.validateRefreshToken(refreshToken)).isTrue();
    assertThat(tokenProvider.getUserId(accessToken)).isEqualTo(userId);
    assertThat(tokenProvider.getTokenId(accessToken)).isNotBlank();
    assertThat(tokenProvider.getAccessTokenExpiry(accessToken)).isAfter(Instant.now());
  }

  @Test
  @DisplayName("Token 검증 실패")
  void invalidToken() {
    String invalidToken = "notToken";

    assertThat(tokenProvider.validateAccessToken(invalidToken)).isFalse();
    assertThat(tokenProvider.validateRefreshToken(invalidToken)).isFalse();
    assertThatThrownBy(() -> tokenProvider.getUserId(invalidToken))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  @DisplayName("Refresh Token 쿠키 생성")
  void refreshTokenCookie() {
    MockHttpServletResponse response = new MockHttpServletResponse();

    tokenProvider.addRefreshCookie(response, "refresh-token");
    tokenProvider.expireRefreshCookie(response);

    assertThat(response.getHeaders("Set-Cookie"))
        .anySatisfy(cookie -> assertThat(cookie)
            .contains("REFRESH_TOKEN=refresh-token")
            .contains("HttpOnly")
            .contains("Secure")
            .contains("Path=/")
            .contains("SameSite=Lax"))
        .anySatisfy(cookie -> assertThat(cookie)
            .contains("REFRESH_TOKEN=")
            .contains("Max-Age=0"));
  }
}
