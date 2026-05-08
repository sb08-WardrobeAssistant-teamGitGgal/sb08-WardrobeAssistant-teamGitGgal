package com.gitggal.clothesplz.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.user.UserRole;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import com.gitggal.clothesplz.security.ClothesUserDetailsService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock
  private JwtTokenProvider tokenProvider;

  @Mock
  private ClothesUserDetailsService userDetailsService;

  @Mock
  private JwtRegistry jwtRegistry;

  private JwtAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthenticationFilter(
        tokenProvider,
        userDetailsService,
        new ObjectMapper(),
        jwtRegistry
    );
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Authorization 헤더가 없으면 인증 없이 다음 필터로 진행")
  void doFilterWithoutAuthorizationHeader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(tokenProvider, never()).validateAccessToken(any());
  }

  @Test
  @DisplayName("유효한 Bearer Token이면 SecurityContext에 인증 정보를 저장")
  void doFilterWithValidAccessToken() throws Exception {
    UUID userId = UUID.randomUUID();
    ClothesUserDetails userDetails = userDetails(userId);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer access-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    when(tokenProvider.validateAccessToken("access-token")).thenReturn(true);
    when(jwtRegistry.hasActiveJwtInformationByAccessToken("access-token")).thenReturn(true);
    when(tokenProvider.getUserId("access-token")).thenReturn(userId);
    when(userDetailsService.loadUserById(userId)).thenReturn(userDetails);

    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
        .isEqualTo(userDetails);
  }

  @Test
  @DisplayName("Access Token 검증 실패")
  void doFilterWithInvalidAccessToken() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer invalid-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    when(tokenProvider.validateAccessToken("invalid-token")).thenReturn(false);

    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString()).contains("JWT_TOKEN_INVALID");
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(filterChain.getRequest()).isNull();
    verify(tokenProvider, never()).getUserId(any());
  }

  @Test
  @DisplayName("Registry에 없는 Access Token")
  void doFilterWithUnregisteredAccessToken() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer access-token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    when(tokenProvider.validateAccessToken("access-token")).thenReturn(true);
    when(jwtRegistry.hasActiveJwtInformationByAccessToken("access-token")).thenReturn(false);

    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentAsString()).contains("JWT_TOKEN_INVALID");
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    assertThat(filterChain.getRequest()).isNull();
    verify(userDetailsService, never()).loadUserById(any(UUID.class));
  }

  private ClothesUserDetails userDetails(UUID userId) {
    UserDto userDto = new UserDto(
        userId,
        Instant.now(),
        "test@test.com",
        "홍길동",
        UserRole.USER,
        false
    );
    return new ClothesUserDetails(userDto, "encoded-password");
  }
}
