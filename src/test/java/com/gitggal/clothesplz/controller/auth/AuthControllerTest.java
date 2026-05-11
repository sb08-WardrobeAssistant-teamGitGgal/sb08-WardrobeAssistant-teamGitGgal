package com.gitggal.clothesplz.controller.auth;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gitggal.clothesplz.config.TestSecurityConfig;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.user.UserRole;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.GlobalExceptionHandler;
import com.gitggal.clothesplz.security.jwt.JwtAuthenticationFilter;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.security.jwt.JwtAuthenticationFilter;
import com.gitggal.clothesplz.security.jwt.JwtInformation;
import com.gitggal.clothesplz.security.jwt.JwtTokenProvider;
import com.gitggal.clothesplz.service.auth.AuthService;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


@Import({
    GlobalExceptionHandler.class,
    TestSecurityConfig.class
})
@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class
        )
    }
)
@DisplayName("AuthController Test")
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuthService authService;

  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @MockitoBean
  private JwtTokenProvider jwtTokenProvider;

  private UserDto userDto;
  private JwtInformation jwtInformation;
  private String accessToken;
  private String refreshToken;

  @BeforeEach
  void setUp() {
    UUID userId = UUID.randomUUID();

    userDto = new UserDto(
        userId,
        Instant.now(),
        "test@test.com",
        "TestUser",
        UserRole.USER,
        false
    );

    accessToken = "new.access.token";
    refreshToken = "new.refresh.token";
    Instant accessTokenExpiry = Instant.now().plusSeconds(3600);
    Instant refreshTokenExpiry = Instant.now().plusSeconds(86400);

    jwtInformation = new JwtInformation(
        userDto,
        accessToken,
        refreshToken,
        accessTokenExpiry,
        refreshTokenExpiry
    );
  }

  @Nested
  @DisplayName("CSRF 토큰 발급")
  class GetCsrfToken {

    @Test
    @DisplayName("성공 - CSRF 토큰 요청 시 204 No Content를 반환한다")
    void success_getCsrfToken() throws Exception {
      // when & then
      mockMvc.perform(get("/api/auth/csrf-token")
              .with(csrf()))
          .andDo(print())
          .andExpect(status().isNoContent());
    }
  }

  @Nested
  @DisplayName("토큰 재발급")
  class RefreshToken {

    @Test
    @DisplayName("토큰 재발급 성공")
    void refreshToken_success() throws Exception {
      // given
      String oldRefreshToken = "old.refresh.token";

      ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken)
          .httpOnly(true)
          .secure(true)
          .path("/")
          .maxAge(86400)
          .sameSite("Lax")
          .build();

      given(authService.refresh(oldRefreshToken)).willReturn(jwtInformation);
      given(jwtTokenProvider.generateRefreshTokenCookie(refreshToken))
          .willReturn(refreshCookie);

      // when & then
      mockMvc.perform(post("/api/auth/refresh")
              .with(csrf())
              .cookie(new Cookie("REFRESH_TOKEN", oldRefreshToken)))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.userDto.id").value(userDto.id().toString()))
          .andExpect(jsonPath("$.userDto.email").value("test@test.com"))
          .andExpect(jsonPath("$.userDto.name").value("TestUser"))
          .andExpect(jsonPath("$.accessToken").value(accessToken))
          .andExpect(cookie().value("REFRESH_TOKEN", refreshToken))
          .andExpect(cookie().httpOnly("REFRESH_TOKEN", true))
          .andExpect(cookie().secure("REFRESH_TOKEN", true))
          .andExpect(cookie().path("REFRESH_TOKEN", "/"))
          .andExpect(cookie().maxAge("REFRESH_TOKEN", 86400));

      verify(authService).refresh(oldRefreshToken);
      verify(jwtTokenProvider).generateRefreshTokenCookie(refreshToken);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 유효하지 않은 Refresh Token")
    void refreshToken_fail_invalidToken() throws Exception {
      // given
      String invalidRefreshToken = "invalid.refresh.token";

      given(authService.refresh(anyString()))
          .willThrow(new BusinessException(UserErrorCode.INVALID_TOKEN));

      // when & then
      mockMvc.perform(post("/api/auth/refresh")
              .with(csrf())
              .cookie(new Cookie("REFRESH_TOKEN", invalidRefreshToken)))
          .andDo(print())
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.exceptionName").value(UserErrorCode.INVALID_TOKEN.name()));

      verify(authService).refresh(invalidRefreshToken);
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 만료된 Refresh Token")
    void refreshToken_fail_expiredToken() throws Exception {
      // given
      String expiredRefreshToken = "expired.refresh.token";

      given(authService.refresh(expiredRefreshToken))
          .willThrow(new BusinessException(UserErrorCode.INVALID_TOKEN));

      // when & then
      mockMvc.perform(post("/api/auth/refresh")
              .with(csrf())
              .cookie(new Cookie("REFRESH_TOKEN", expiredRefreshToken)))
          .andDo(print())
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.exceptionName").value(UserErrorCode.INVALID_TOKEN.name()));

      verify(authService).refresh(expiredRefreshToken);
    }
  }
}