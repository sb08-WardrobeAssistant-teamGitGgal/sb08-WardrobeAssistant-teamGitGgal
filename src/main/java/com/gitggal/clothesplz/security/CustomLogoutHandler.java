package com.gitggal.clothesplz.security;

import com.gitggal.clothesplz.security.jwt.JwtRegistry;
import com.gitggal.clothesplz.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {

  private final JwtTokenProvider tokenProvider;
  private final JwtRegistry jwtRegistry;

  @Override
  public void logout(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) {
    if (!isAuthenticated(authentication)) {
      log.warn("[Security] 로그아웃: 인증되지 않은 사용자입니다.");
      return;
    }

    ResponseCookie responseCookie = tokenProvider.generateRefreshTokenExpirationCookie();
    response.addHeader("Set-Cookie", responseCookie.toString());

    Object principal = authentication.getPrincipal();
    if (!(principal instanceof ClothesUserDetails userDetails)) {
      log.warn("[Security] 로그아웃: 지원하지 않는 principal 타입입니다. type={}",
          principal == null ? "null" : principal.getClass().getName());
      return;
    }

    UUID userId = userDetails.getUserDto().id();
    jwtRegistry.invalidateJwtInformationByUserId(userId);

  }

  private boolean isAuthenticated(Authentication authentication) {
    return authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken);
  }
}
