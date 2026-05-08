package com.gitggal.clothesplz.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.user.UserRole;
import com.gitggal.clothesplz.exception.ErrorResponse;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.security.jwt.JwtRegistry;
import com.gitggal.clothesplz.security.jwt.JwtTokenProvider;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class LoginSuccessHandlerTest {

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
  private LoginSuccessHandler loginSuccessHandler;

  private ClothesUserDetails userDetails;

  @BeforeEach
  void setUp() {

    UserDto userDto = new UserDto(
        UUID.randomUUID(),
        Instant.now(),
        "test@test.com",
        "홍길동",
        UserRole.USER,
        false
    );

    userDetails = new ClothesUserDetails(userDto, "encoded-password");
  }

  @Test
  @DisplayName("JWT 생성 ")
  void onAuthenticationSuccess() throws Exception {

    when(authentication.getPrincipal()).thenReturn(userDetails);
    when(tokenProvider.generateAccessToken(any()))
        .thenReturn("access-token");
    when(tokenProvider.generateRefreshToken(any()))
        .thenReturn("refresh-token");
    when(tokenProvider.getAccessTokenExpiry(any()))
        .thenReturn(Instant.now().plusSeconds(1800));
    when(tokenProvider.getRefreshTokenExpiry(any()))
        .thenReturn(Instant.now().plusSeconds(604800));
    when(objectMapper.writeValueAsString(any()))
        .thenReturn("{}");

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);

    when(response.getWriter()).thenReturn(writer);

    loginSuccessHandler.onAuthenticationSuccess(
        request,
        response,
        authentication
    );

    verify(jwtRegistry).registerJwtInformation(any());
    verify(tokenProvider).addRefreshCookie(any(), any());
    verify(response).setStatus(HttpServletResponse.SC_OK);
  }

  @Test
  @DisplayName("JWT 생성 실패")
  void onAuthenticationSuccess_JwtGenerationFailed() throws Exception {
    when(authentication.getPrincipal()).thenReturn(userDetails);
    when(tokenProvider.generateAccessToken(any()))
        .thenThrow(new JOSEException("failed"));

    ErrorResponse errorResponse = ErrorResponse.of(UserErrorCode.JWT_TOKEN_GENERATION_FAILED);
    when(objectMapper.writeValueAsString(errorResponse)).thenReturn("{}");
    when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

    loginSuccessHandler.onAuthenticationSuccess(request, response, authentication);

    verify(jwtRegistry, never()).registerJwtInformation(any());
    verify(tokenProvider, never()).addRefreshCookie(any(), any());
    verify(jwtRegistry, never()).invalidateJwtInformationByUserId(any());
    verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }

  @Test
  @DisplayName("인증 실패")
  void onAuthenticationSuccess_InvalidPrincipal() throws Exception {
    when(authentication.getPrincipal()).thenReturn("not");

    ErrorResponse errorResponse = ErrorResponse.of(UserErrorCode.AUTHENTICATION_PRINCIPAL_INVALID);
    when(objectMapper.writeValueAsString(errorResponse)).thenReturn("{}");
    when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

    loginSuccessHandler.onAuthenticationSuccess(request, response, authentication);

    verify(jwtRegistry, never()).invalidateJwtInformationByUserId(any());
    verify(jwtRegistry, never()).registerJwtInformation(any());
    verify(tokenProvider, never()).addRefreshCookie(any(), any());
    verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  }
}
