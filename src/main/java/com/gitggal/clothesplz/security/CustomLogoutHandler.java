package com.gitggal.clothesplz.security;

import com.gitggal.clothesplz.security.jwt.JwtRegistry;
import com.gitggal.clothesplz.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {

  private final JwtTokenProvider tokenProvider;
  private final JwtRegistry jwtRegistry;

  @Override
  public void logout(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) {
    ResponseCookie responseCookie = tokenProvider.generateRefreshTokenExpirationCookie();
    response.addHeader("Set-Cookie", responseCookie.toString());

    Arrays.stream(request.getCookies())
        .filter(cookie -> cookie.getName().equals(JwtTokenProvider.REFRESH_TOKEN_COOKIE_NAME))
        .findFirst()
        .ifPresent(cookie -> {
          String refreshToken = cookie.getValue();
          UUID userId = tokenProvider.getUserId(refreshToken);
          jwtRegistry.invalidateJwtInformationByUserId(userId);
        });

  }
}
