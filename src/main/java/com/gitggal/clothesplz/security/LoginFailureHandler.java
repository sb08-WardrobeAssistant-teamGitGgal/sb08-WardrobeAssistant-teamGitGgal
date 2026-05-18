package com.gitggal.clothesplz.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.exception.ErrorResponse;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception) throws IOException, ServletException {

    ErrorResponse errorResponse;

    if (exception instanceof LockedException) {
      errorResponse = ErrorResponse.of(UserErrorCode.ACCOUNT_LOCKED);
    } else {
      errorResponse = ErrorResponse.of(UserErrorCode.AUTHENTICATION_FAILED);
    }

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
  }
}
