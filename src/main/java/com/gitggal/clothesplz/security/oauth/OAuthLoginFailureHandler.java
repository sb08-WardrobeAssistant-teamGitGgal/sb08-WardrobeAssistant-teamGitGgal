package com.gitggal.clothesplz.security.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

  @Value("${clothesplz.oauth.redirect-uri}")
  private String redirectUri;

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException exception
  ) throws IOException, ServletException {

    log.error("[OAuth] OAuth2 인증 실패 - 오류: {}", exception.getMessage());

    String errorCode = "oauth_authentication_failed";
    String errorMessage = "OAuth 로그인에 실패했습니다.";

    String encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
    String targetUrl =
        redirectUri + "#/auth/login?error=" + errorCode + "&error_message=" + encodedMessage;

    getRedirectStrategy().sendRedirect(request, response, targetUrl);
  }
}