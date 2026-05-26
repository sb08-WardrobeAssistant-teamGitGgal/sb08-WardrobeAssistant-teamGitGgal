package com.gitggal.clothesplz.security.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthLoginFailureHandlerTest {

  @InjectMocks
  private OAuthLoginFailureHandler handler;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private AuthenticationException exception;

  @Test
  void oauth_failure_redirect_success() throws Exception {

    // given
    String redirectUri = "http://localhost:8080";
    ReflectionTestUtils.setField(handler, "redirectUri", redirectUri);

    given(exception.getMessage()).willReturn("error");

    // when
    handler.onAuthenticationFailure(request, response, exception);

    // then
    then(response).should().encodeRedirectURL(
        contains("oauth_authentication_failed")
    );

    then(response).should().encodeRedirectURL(
        contains("error_message")
    );
  }
}