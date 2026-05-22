package com.gitggal.clothesplz.security;

import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.security.jwt.JwtInformation;
import com.gitggal.clothesplz.security.jwt.JwtRegistry;
import com.gitggal.clothesplz.security.jwt.JwtTokenProvider;
import com.gitggal.clothesplz.security.oauth.OAuthInformation;
import com.gitggal.clothesplz.security.oauth.OAuthUserInfoFactory;
import com.gitggal.clothesplz.security.oauth.OAuthUserInformation;
import com.gitggal.clothesplz.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final JwtTokenProvider jwtTokenProvider;
  private final JwtRegistry jwtRegistry;
  private final UserService userService;

  @Value("${clothesplz.oauth2.redirect-uri}")
  private String redirectUri;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) throws IOException {

    try {
      OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
      String registrationId = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();

      OAuthUserInformation userInfo = OAuthUserInfoFactory.getOAuth2UserInfo(
          registrationId,
          oauth2User.getAttributes()
      );

      OAuthInformation info = userInfo.toOAuthInformation();
      ClothesUserDetails userDetails = userService.processOAuth2User(info);
      String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
      String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);
      Instant accessExpiry = jwtTokenProvider.getAccessTokenExpiry(accessToken);
      Instant refreshExpiry = jwtTokenProvider.getRefreshTokenExpiry(refreshToken);

      response.setHeader("Authorization", "Bearer " + accessToken);

      UserDto userDto = userDetails.getUserDto();
      JwtInformation jwtInformation = new JwtInformation(userDto, accessToken, refreshToken,
          accessExpiry, refreshExpiry);
      jwtRegistry.registerJwtInformation(jwtInformation);
      jwtTokenProvider.addRefreshCookie(response, refreshToken);

      getRedirectStrategy().sendRedirect(request, response, redirectUri);
    } catch (Exception e) {

      log.error("[OAuth] 로그인 처리 중 오류 발생", e);

      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");
    }
  }
}
