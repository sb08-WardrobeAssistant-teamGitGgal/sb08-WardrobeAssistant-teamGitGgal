package com.gitggal.clothesplz.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gitggal.clothesplz.dto.user.ResetPasswordRequest;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.user.UserRole;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.repository.user.UserRepository;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import com.gitggal.clothesplz.security.ClothesUserDetailsService;
import com.gitggal.clothesplz.security.jwt.JwtInformation;
import com.gitggal.clothesplz.security.jwt.JwtRegistry;
import com.gitggal.clothesplz.security.jwt.JwtTokenProvider;
import com.nimbusds.jose.JOSEException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Test")
class AuthServiceTest {

  @Mock
  private JwtRegistry jwtRegistry;

  @Mock
  private JwtTokenProvider tokenProvider;

  @Mock
  private ClothesUserDetailsService clothesUserDetailsService;

  @Mock
  private JavaMailSender javaMailSender;

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private AuthServiceImpl authService;

  private UUID userId;
  private UserDto userDto;
  private ClothesUserDetails clothesUserDetails;
  private String oldRefreshToken;
  private String newAccessToken;
  private String newRefreshToken;
  private Instant accessTokenExpiry;
  private Instant refreshTokenExpiry;
  private User user;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();

    userDto = new UserDto(
        userId,
        Instant.now(),
        "test@test.com",
        "TestUser",
        UserRole.USER,
        false
    );

    user = new User(
        "홍길동",
        "test@test.com",
        "oldPassword"
    );

    clothesUserDetails = new ClothesUserDetails(userDto, "password",null,null);

    oldRefreshToken = "old.refresh.token";
    newAccessToken = "new.access.token";
    newRefreshToken = "new.refresh.token";
    accessTokenExpiry = Instant.now().plusSeconds(3600);
    refreshTokenExpiry = Instant.now().plusSeconds(86400);
  }

  @Nested
  @DisplayName("토큰 재발급")
  class Refresh {

    @Test
    @DisplayName("성공")
    void refresh_success() throws JOSEException {
      // given
      given(tokenProvider.validateRefreshToken(oldRefreshToken)).willReturn(true);
      given(jwtRegistry.hasActiveJwtInformationByRefreshToken(oldRefreshToken)).willReturn(true);
      given(tokenProvider.getUsernameFromToken(oldRefreshToken)).willReturn(userId.toString());
      given(clothesUserDetailsService.loadUserById(userId)).willReturn(clothesUserDetails);
      given(tokenProvider.generateAccessToken(clothesUserDetails)).willReturn(newAccessToken);
      given(tokenProvider.generateRefreshToken(clothesUserDetails)).willReturn(newRefreshToken);
      given(tokenProvider.getAccessTokenExpiry(newAccessToken)).willReturn(accessTokenExpiry);
      given(tokenProvider.getRefreshTokenExpiry(newRefreshToken)).willReturn(refreshTokenExpiry);

      // when
      JwtInformation result = authService.refresh(oldRefreshToken);

      // then
      assertThat(result).isNotNull();
      assertThat(result.userDto()).isEqualTo(userDto);
      assertThat(result.accessToken()).isEqualTo(newAccessToken);
      assertThat(result.refreshToken()).isEqualTo(newRefreshToken);
      assertThat(result.accessTokenExpiry()).isEqualTo(accessTokenExpiry);
      assertThat(result.refreshTokenExpiry()).isEqualTo(refreshTokenExpiry);

      verify(tokenProvider).validateRefreshToken(oldRefreshToken);
      verify(jwtRegistry).hasActiveJwtInformationByRefreshToken(oldRefreshToken);
      verify(tokenProvider).getUsernameFromToken(oldRefreshToken);
      verify(clothesUserDetailsService).loadUserById(userId);
      verify(tokenProvider).generateAccessToken(clothesUserDetails);
      verify(tokenProvider).generateRefreshToken(clothesUserDetails);
      verify(tokenProvider).getAccessTokenExpiry(newAccessToken);
      verify(tokenProvider).getRefreshTokenExpiry(newRefreshToken);
      verify(jwtRegistry).rotateJwtInformation(oldRefreshToken, result);
    }

    @Test
    @DisplayName("실패 - Refresh Token 유효성 검증 실패")
    void refresh_fail_invalidRefreshToken() {
      // given
      given(tokenProvider.validateRefreshToken(oldRefreshToken)).willReturn(false);

      // when & then
      assertThatThrownBy(() -> authService.refresh(oldRefreshToken))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.INVALID_TOKEN);

      verify(tokenProvider).validateRefreshToken(oldRefreshToken);
      verify(jwtRegistry, never()).hasActiveJwtInformationByRefreshToken(anyString());
      verify(tokenProvider, never()).getUsernameFromToken(anyString());
      verify(clothesUserDetailsService, never()).loadUserById(any());
      verify(jwtRegistry, never()).rotateJwtInformation(anyString(), any());
    }

    @Test
    @DisplayName("실패 - Registry에 활성 토큰 없음")
    void refresh_fail_noActiveToken() {
      // given
      given(tokenProvider.validateRefreshToken(oldRefreshToken)).willReturn(true);
      given(jwtRegistry.hasActiveJwtInformationByRefreshToken(oldRefreshToken)).willReturn(false);

      // when & then
      assertThatThrownBy(() -> authService.refresh(oldRefreshToken))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.INVALID_TOKEN);

      verify(tokenProvider).validateRefreshToken(oldRefreshToken);
      verify(jwtRegistry).hasActiveJwtInformationByRefreshToken(oldRefreshToken);
      verify(tokenProvider, never()).getUsernameFromToken(anyString());
      verify(clothesUserDetailsService, never()).loadUserById(any());
      verify(jwtRegistry, never()).rotateJwtInformation(anyString(), any());
    }

    @Test
    @DisplayName("실패 - 사용자를 찾을 수 없음")
    void refresh_fail_userNotFound() throws JOSEException {
      // given
      given(tokenProvider.validateRefreshToken(oldRefreshToken)).willReturn(true);
      given(jwtRegistry.hasActiveJwtInformationByRefreshToken(oldRefreshToken)).willReturn(true);
      given(tokenProvider.getUsernameFromToken(oldRefreshToken)).willReturn(userId.toString());
      given(clothesUserDetailsService.loadUserById(userId))
          .willThrow(new BusinessException(UserErrorCode.USER_NOT_FOUND));

      // when & then
      assertThatThrownBy(() -> authService.refresh(oldRefreshToken))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);

      verify(tokenProvider).validateRefreshToken(oldRefreshToken);
      verify(jwtRegistry).hasActiveJwtInformationByRefreshToken(oldRefreshToken);
      verify(tokenProvider).getUsernameFromToken(oldRefreshToken);
      verify(clothesUserDetailsService).loadUserById(userId);
      verify(tokenProvider, never()).generateAccessToken(any());
      verify(jwtRegistry, never()).rotateJwtInformation(anyString(), any());
    }

    @Test
    @DisplayName("실패 - JWT 생성 중 JOSEException 발생")
    void refresh_fail_joseException() throws JOSEException {
      // given
      given(tokenProvider.validateRefreshToken(oldRefreshToken)).willReturn(true);
      given(jwtRegistry.hasActiveJwtInformationByRefreshToken(oldRefreshToken)).willReturn(true);
      given(tokenProvider.getUsernameFromToken(oldRefreshToken)).willReturn(userId.toString());
      given(clothesUserDetailsService.loadUserById(userId)).willReturn(clothesUserDetails);
      given(tokenProvider.generateAccessToken(clothesUserDetails))
          .willThrow(new JOSEException("JWT generation failed"));

      // when & then
      assertThatThrownBy(() -> authService.refresh(oldRefreshToken))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.JWT_TOKEN_GENERATION_FAILED);

      verify(tokenProvider).validateRefreshToken(oldRefreshToken);
      verify(jwtRegistry).hasActiveJwtInformationByRefreshToken(oldRefreshToken);
      verify(tokenProvider).getUsernameFromToken(oldRefreshToken);
      verify(clothesUserDetailsService).loadUserById(userId);
      verify(tokenProvider).generateAccessToken(clothesUserDetails);
      verify(jwtRegistry, never()).rotateJwtInformation(anyString(), any());
    }
  }

  @Nested
  @DisplayName("임시 비밀번호 발급")
  class sendTempPassword{

    @Test
    @DisplayName("성공")
    void success_sendTempPassword() {
      // given
      ResetPasswordRequest request =
          new ResetPasswordRequest("test@test.com");

      given(userRepository.findByEmail(request.email()))
          .willReturn(Optional.of(user));

      given(passwordEncoder.encode(anyString()))
          .willReturn("encodedTempPassword");

      // when
      authService.sendTempPassword(request);

      // then
      assertThat(user.getTempPassword()).isEqualTo("encodedTempPassword");
      assertThat(user.getTempPasswordExpiresAt()).isNotNull();

      verify(userRepository).findByEmail(request.email());
      verify(passwordEncoder).encode(anyString());
      verify(javaMailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("실패 - 사용자를 찾을 수 없음")
    void fail_userNotFound() {
      // given
      ResetPasswordRequest request =
          new ResetPasswordRequest("test@test.com");

      given(userRepository.findByEmail(request.email()))
          .willReturn(Optional.empty());

      // when & then
      authService.sendTempPassword(request);

      verify(userRepository).findByEmail(request.email());
      verify(passwordEncoder, never()).encode(anyString());
      verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }
  }
}