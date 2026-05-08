package com.gitggal.clothesplz.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.exception.ErrorResponse;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.security.jwt.JwtDto;
import com.gitggal.clothesplz.security.jwt.JwtInformation;
import com.gitggal.clothesplz.security.jwt.JwtRegistry;
import com.gitggal.clothesplz.security.jwt.JwtTokenProvider;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

  private final ObjectMapper objectMapper;
  private final JwtTokenProvider tokenProvider;
  private final JwtRegistry jwtRegistry;

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {

    response.setCharacterEncoding("UTF-8");
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

    if (authentication.getPrincipal() instanceof ClothesUserDetails userDetails) {
      try {
        String accessToken = tokenProvider.generateAccessToken(userDetails);
        String refreshToken = tokenProvider.generateRefreshToken(userDetails);
        Instant accessExpiry = tokenProvider.getAccessTokenExpiry(accessToken);
        Instant refreshExpiry = tokenProvider.getRefreshTokenExpiry(refreshToken);

        UserDto userDto = userDetails.getUserDto();

        JwtInformation jwtInformation = new JwtInformation(userDto, accessToken, refreshToken, accessExpiry, refreshExpiry);
        jwtRegistry.invalidateJwtInformationByUserId(userDetails.getUserDto().id());
        jwtRegistry.registerJwtInformation(jwtInformation);
        tokenProvider.addRefreshCookie(response, refreshToken);

        JwtDto jwtDto = new JwtDto(userDto, accessToken);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(objectMapper.writeValueAsString(jwtDto));

      } catch (JOSEException e) {
        ErrorResponse errorResponse = ErrorResponse.of(UserErrorCode.JWT_TOKEN_GENERATION_FAILED);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
      }
    } else {
      ErrorResponse errorResponse = ErrorResponse.of(
          UserErrorCode.AUTHENTICATION_PRINCIPAL_INVALID);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
  }
}
