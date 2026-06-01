package com.gitggal.clothesplz.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.user.UserRole;
import com.gitggal.clothesplz.security.jwt.JwtRegistry;
import com.gitggal.clothesplz.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class CustomLogoutHandlerTest {

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

  @Mock
  private AnonymousAuthenticationToken anonymousAuthentication;

  private CustomLogoutHandler logoutHandler;
  private UUID userId;
  private ClothesUserDetails userDetails;

  @BeforeEach
  void setUp() {
    logoutHandler = new CustomLogoutHandler(tokenProvider, jwtRegistry);
    userId = UUID.randomUUID();
    UserDto userDto = new UserDto(userId, Instant.now(), "test@test.com", "tester", UserRole.USER,
        false);
    userDetails = new ClothesUserDetails(userDto, "password");
  }

  @Test
  @DisplayName("로그아웃 성공 - 인증된 사용자")
  void logout_authenticated() {

    given(authentication.isAuthenticated()).willReturn(true);
    given(authentication.getPrincipal()).willReturn(userDetails);
    given(tokenProvider.generateRefreshTokenExpirationCookie())
        .willReturn(ResponseCookie.from("refreshToken", "").maxAge(0).build());

    logoutHandler.logout(request, response, authentication);

    verify(response).addHeader(any(), any());
    verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
  }

  @Test
  @DisplayName("로그아웃 성공 - refresh token이 만료")
  void logout_withExpiredRefreshToken_stillInvalidatesServerSide() {

    given(authentication.isAuthenticated()).willReturn(true);
    given(authentication.getPrincipal()).willReturn(userDetails);
    given(tokenProvider.generateRefreshTokenExpirationCookie())
        .willReturn(ResponseCookie.from("refreshToken", "").maxAge(0).build());

    logoutHandler.logout(request, response, authentication);

    verify(tokenProvider, never()).validateRefreshToken(any());
    verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
  }

  @Test
  @DisplayName("로그아웃 실패 - 인증이 없을 경우")
  void logout_nullAuthentication() {

    logoutHandler.logout(request, response, null);

    verify(jwtRegistry, never()).invalidateJwtInformationByUserId(any());
    verify(tokenProvider, never()).generateRefreshTokenExpirationCookie();
  }

  @Test
  @DisplayName("로그아웃 실패 - 인증되지 않은 사용자")
  void logout_notAuthenticated() {

    given(authentication.isAuthenticated()).willReturn(false);

    logoutHandler.logout(request, response, authentication);

    verify(jwtRegistry, never()).invalidateJwtInformationByUserId(any());
    verify(tokenProvider, never()).generateRefreshTokenExpirationCookie();
  }

  @Test
  @DisplayName("로그아웃 실패 - 익명 사용자")
  void logout_anonymousAuthentication() {

    given(anonymousAuthentication.isAuthenticated()).willReturn(true);

    logoutHandler.logout(request, response, anonymousAuthentication);

    verify(jwtRegistry, never()).invalidateJwtInformationByUserId(any());
    verify(tokenProvider, never()).generateRefreshTokenExpirationCookie();
  }

  @Test
  @DisplayName("로그아웃 - 인증 타입이 다를 경우")
  void logout_unsupportedPrincipalType() {
    given(authentication.isAuthenticated()).willReturn(true);
    given(authentication.getPrincipal()).willReturn("unexpected-string-principal");
    given(tokenProvider.generateRefreshTokenExpirationCookie())
        .willReturn(ResponseCookie.from("refreshToken", "").maxAge(0).build());

    logoutHandler.logout(request, response, authentication);

    verify(response).addHeader(any(), any());
    verify(jwtRegistry, never()).invalidateJwtInformationByUserId(any());
  }
}
