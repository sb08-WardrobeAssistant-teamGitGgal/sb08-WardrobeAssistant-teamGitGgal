package com.gitggal.clothesplz.security;

import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.contains;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.then;

import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.security.jwt.JwtInformation;
import com.gitggal.clothesplz.security.jwt.JwtRegistry;
import com.gitggal.clothesplz.security.jwt.JwtTokenProvider;
import com.gitggal.clothesplz.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuthLoginSuccessHandlerTest {

  @InjectMocks
  private OAuthLoginSuccessHandler handler;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private JwtRegistry jwtRegistry;

  @Mock
  private UserService userService;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private OAuth2AuthenticationToken authentication;

  @Mock
  private OAuth2User oAuth2User;

  private final String redirectUri = "http://localhost:8080";
  private Map<String, Object> attributes;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(handler, "redirectUri", redirectUri);

    attributes = Map.of(
        "sub", "12345",
        "email", "test@test.com",
        "name", "test"
    );
  }

  @Test
  @DisplayName("OAuth 로그인 성공")
  void success() throws Exception {

    // given
    ClothesUserDetails userDetails = mock(ClothesUserDetails.class);
    UserDto userDto = mock(UserDto.class);

    given(authentication.getPrincipal()).willReturn(oAuth2User);
    given(authentication.getAuthorizedClientRegistrationId()).willReturn("google");
    given(oAuth2User.getAttributes()).willReturn(attributes);

    given(userService.processOAuth2User(any())).willReturn(userDetails);

    given(userDetails.getUserDto()).willReturn(userDto);

    given(jwtTokenProvider.generateAccessToken(any())).willReturn("access");
    given(jwtTokenProvider.generateRefreshToken(any())).willReturn("refresh");
    given(jwtTokenProvider.getAccessTokenExpiry(anyString())).willReturn(Instant.now());
    given(jwtTokenProvider.getRefreshTokenExpiry(anyString())).willReturn(Instant.now());

    // when
    handler.onAuthenticationSuccess(request, response, authentication);

    // then
    then(jwtRegistry).should().registerJwtInformation(any(JwtInformation.class));
    then(jwtTokenProvider).should().addRefreshCookie(response, "refresh");
  }

  @Test
  @DisplayName("OAuth 로그인 실패 - Locked")
  void locked() throws Exception {

    // given
    given(authentication.getPrincipal()).willReturn(oAuth2User);
    given(authentication.getAuthorizedClientRegistrationId()).willReturn("google");
    given(oAuth2User.getAttributes()).willReturn(attributes);

    given(userService.processOAuth2User(any())).willThrow(new LockedException("locked"));

    // when
    handler.onAuthenticationSuccess(request, response, authentication);

    // then
    then(response).should().encodeRedirectURL(contains("auth/login"));
  }

  @Test
  @DisplayName("OAuth 로그인 실패 - 예외")
  void fail() throws Exception {

    // given
    given(authentication.getPrincipal()).willReturn(oAuth2User);
    given(authentication.getAuthorizedClientRegistrationId()).willReturn("google");
    given(oAuth2User.getAttributes()).willReturn(attributes);

    given(userService.processOAuth2User(any())).willThrow(new RuntimeException("error"));

    // when
    handler.onAuthenticationSuccess(request, response, authentication);

    // then
    then(response).should().encodeRedirectURL(contains("oauth_failed"));
  }
}