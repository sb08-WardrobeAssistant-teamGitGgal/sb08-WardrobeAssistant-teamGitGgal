package com.gitggal.clothesplz.security;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.security.jwt.JwtRegistry;
import com.gitggal.clothesplz.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
public class CustomLogoutHandlerTest {

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private JwtTokenProvider tokenProvider;

  @Mock
  private JwtRegistry jwtRegistry;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private Authentication authentication;

  @InjectMocks
  private CustomLogoutHandler customLogoutHandler;

  private UUID userId;
  private String refreshToken;
  private ResponseCookie expiredCookie;

  @BeforeEach
  void setUp() {
    refreshToken = "refresh-token";
    userId = UUID.randomUUID();
    expiredCookie = ResponseCookie.from("REFRESH_TOKEN", "")
        .maxAge(0)
        .path("/")
        .httpOnly(true)
        .secure(true)
        .build();
  }

  @Test
  @DisplayName("로그아웃 성공")
  void logoutSuccess() {

    Cookie cookie = new Cookie("REFRESH_TOKEN", refreshToken);

    when(tokenProvider.generateRefreshTokenExpirationCookie()).thenReturn(expiredCookie);
    when(request.getCookies()).thenReturn(new Cookie[]{cookie});
    when(tokenProvider.validateRefreshToken(refreshToken)).thenReturn(true);
    when(tokenProvider.getUserId(refreshToken)).thenReturn(userId);

    customLogoutHandler.logout(request, response, authentication);

    verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
    verify(response).addHeader(eq("Set-Cookie"), anyString());
    verify(tokenProvider).validateRefreshToken(refreshToken);
    verify(tokenProvider).getUserId(refreshToken);
    verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
  }

  @Nested
  @DisplayName("로그아웃 실패")
  class LogoutFailure{

    @Test
    @DisplayName("쿠키가 없는 경우")
    void logout_fail_noCookies(){
      when(tokenProvider.generateRefreshTokenExpirationCookie()).thenReturn(expiredCookie);
      when(request.getCookies()).thenReturn(null);

      customLogoutHandler.logout(request, response, authentication);

      verify(response).addHeader(eq("Set-Cookie"), anyString());
      verify(jwtRegistry, never()).invalidateJwtInformationByUserId(userId);
    }

    @Test
    @DisplayName("refresh Token이 없는 경우")
    void logout_fail_noRefreshTokenCookie(){
      Cookie otherCookie = new Cookie("OTHER_COOKIE", "other-value");

      when(tokenProvider.generateRefreshTokenExpirationCookie()).thenReturn(expiredCookie);
      when(request.getCookies()).thenReturn(new Cookie[]{otherCookie});

      customLogoutHandler.logout(request, response, authentication);

      verify(response).addHeader(eq("Set-Cookie"), anyString());
      verify(jwtRegistry, never()).invalidateJwtInformationByUserId(userId);
      verify(jwtRegistry, never()).invalidateJwtInformationByUserId(userId);
    }

    @Test
    @DisplayName("유효하지 않은 Refresh Token일 경우")
    void logout_fail_invalidRefreshToken() {
      String invalidToken = "invalid.refresh.token";
      Cookie cookie = new Cookie("REFRESH_TOKEN", invalidToken);

      when(tokenProvider.generateRefreshTokenExpirationCookie()).thenReturn(expiredCookie);
      when(request.getCookies()).thenReturn(new Cookie[]{cookie});
      when(tokenProvider.validateRefreshToken(invalidToken)).thenReturn(false);

      customLogoutHandler.logout(request, response, authentication);

      verify(response).addHeader(eq("Set-Cookie"), anyString());
      verify(tokenProvider).validateRefreshToken(invalidToken);
      verify(tokenProvider, never()).getUserId(anyString());
      verify(jwtRegistry, never()).invalidateJwtInformationByUserId(userId);
    }
  }
}
