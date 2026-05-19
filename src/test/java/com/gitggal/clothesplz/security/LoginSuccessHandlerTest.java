package com.gitggal.clothesplz.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

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

    userDetails = new ClothesUserDetails(userDto, "encoded-password", null, null);
  }

  @Test
  @DisplayName("JWT 생성")
  void onAuthenticationSuccess() throws Exception {

    given(authentication.getPrincipal()).willReturn(userDetails);
    given(tokenProvider.generateAccessToken(any())).willReturn("access-token");
    given(tokenProvider.generateRefreshToken(any())).willReturn("refresh-token");
    given(tokenProvider.getAccessTokenExpiry(any())).willReturn(Instant.now().plusSeconds(1800));
    given(tokenProvider.getRefreshTokenExpiry(any())).willReturn(Instant.now().plusSeconds(604800));
    given(objectMapper.writeValueAsString(any())).willReturn("{}");

    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);

    given(response.getWriter()).willReturn(writer);

    loginSuccessHandler.onAuthenticationSuccess(
        request,
        response,
        authentication
    );

    then(response).should().setStatus(HttpServletResponse.SC_OK);
    then(jwtRegistry).should().registerJwtInformation(any());
    then(tokenProvider).should().addRefreshCookie(any(), any());
  }

  @Test
  @DisplayName("JWT 생성 실패")
  void onAuthenticationSuccess_JwtGenerationFailed() throws Exception {
    given(authentication.getPrincipal()).willReturn(userDetails);
    given(tokenProvider.generateAccessToken(any())).willThrow(new JOSEException("failed"));

    ErrorResponse errorResponse = ErrorResponse.of(UserErrorCode.JWT_TOKEN_GENERATION_FAILED);
    given(objectMapper.writeValueAsString(errorResponse)).willReturn("{}");
    given(response.getWriter()).willReturn(new PrintWriter(new StringWriter()));

    loginSuccessHandler.onAuthenticationSuccess(request, response, authentication);

    then(response).should().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    then(jwtRegistry).should(never()).registerJwtInformation(any());
    then(tokenProvider).should(never()).addRefreshCookie(any(), any());
  }

  @Test
  @DisplayName("인증 실패")
  void onAuthenticationSuccess_InvalidPrincipal() throws Exception {
    given(authentication.getPrincipal()).willReturn("not");

    ErrorResponse errorResponse = ErrorResponse.of(UserErrorCode.AUTHENTICATION_PRINCIPAL_INVALID);
    given(objectMapper.writeValueAsString(errorResponse)).willReturn("{}");
    given(response.getWriter()).willReturn(new PrintWriter(new StringWriter()));

    loginSuccessHandler.onAuthenticationSuccess(request, response, authentication);

    then(response).should().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    then(jwtRegistry).should(never()).registerJwtInformation(any());
    then(tokenProvider).should(never()).addRefreshCookie(any(), any());
  }
}
