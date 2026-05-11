package com.gitggal.clothesplz.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.user.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InMemoryJwtRegistryTest {

  @Mock
  private JwtTokenProvider tokenProvider;

  private InMemoryJwtRegistry jwtRegistry;
  private UserDto userDto;

  @BeforeEach
  void setUp() {
    jwtRegistry = new InMemoryJwtRegistry(tokenProvider);
    userDto = new UserDto(
        UUID.randomUUID(),
        Instant.now(),
        "test@test.com",
        "홍길동",
        UserRole.USER,
        false
    );
  }

  @Test
  @DisplayName("JWT 등록")
  void registerJwtInformation() {
    JwtInformation jwtInformation = jwtInformation("access-token", "refresh-token");
    when(tokenProvider.validateAccessToken("access-token")).thenReturn(true);
    when(tokenProvider.validateRefreshToken("refresh-token")).thenReturn(true);

    jwtRegistry.registerJwtInformation(jwtInformation);

    assertThat(jwtRegistry.hasActiveJwtInformationByUserId(userDto.id())).isTrue();
    assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken("access-token")).isTrue();
    assertThat(jwtRegistry.hasActiveJwtInformationByRefreshToken("refresh-token")).isTrue();
  }

  @Test
  @DisplayName("같은 사용자가 새 JWT를 등록하면 기존 JWT 인덱스는 제거")
  void registerJwtInformationRemovesOldTokenIndexes() {
    jwtRegistry.registerJwtInformation(jwtInformation("old-access-token", "old-refresh-token"));
    jwtRegistry.registerJwtInformation(jwtInformation("new-access-token", "new-refresh-token"));

    when(tokenProvider.validateAccessToken("new-access-token")).thenReturn(true);
    when(tokenProvider.validateRefreshToken("new-refresh-token")).thenReturn(true);

    assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken("old-access-token")).isFalse();
    assertThat(jwtRegistry.hasActiveJwtInformationByRefreshToken("old-refresh-token")).isFalse();
    assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken("new-access-token")).isTrue();
    assertThat(jwtRegistry.hasActiveJwtInformationByRefreshToken("new-refresh-token")).isTrue();
  }

  @Test
  @DisplayName("JWT 무효화")
  void invalidateJwtInformationByUserId() {
    jwtRegistry.registerJwtInformation(jwtInformation("access-token", "refresh-token"));

    jwtRegistry.invalidateJwtInformationByUserId(userDto.id());

    assertThat(jwtRegistry.hasActiveJwtInformationByUserId(userDto.id())).isFalse();
    assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken("access-token")).isFalse();
    assertThat(jwtRegistry.hasActiveJwtInformationByRefreshToken("refresh-token")).isFalse();
  }

  @Test
  @DisplayName("JWT 교체")
  void rotateJwtInformation() {
    jwtRegistry.registerJwtInformation(jwtInformation("old-access-token", "old-refresh-token"));
    jwtRegistry.rotateJwtInformation(
        "old-refresh-token",
        jwtInformation("new-access-token", "new-refresh-token")
    );

    when(tokenProvider.validateAccessToken("new-access-token")).thenReturn(true);
    when(tokenProvider.validateRefreshToken("new-refresh-token")).thenReturn(true);

    assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken("old-access-token")).isFalse();
    assertThat(jwtRegistry.hasActiveJwtInformationByRefreshToken("old-refresh-token")).isFalse();
    assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken("new-access-token")).isTrue();
    assertThat(jwtRegistry.hasActiveJwtInformationByRefreshToken("new-refresh-token")).isTrue();
  }

  @Test
  @DisplayName("만료된 JWT 정리")
  void clearExpiredJwtInformation() {
    jwtRegistry.registerJwtInformation(jwtInformation("access-token", "refresh-token"));
    when(tokenProvider.validateAccessToken("access-token")).thenReturn(false);

    jwtRegistry.clearExpiredJwtInformation();

    assertThat(jwtRegistry.hasActiveJwtInformationByAccessToken("access-token")).isFalse();
    assertThat(jwtRegistry.hasActiveJwtInformationByRefreshToken("refresh-token")).isFalse();
  }

  private JwtInformation jwtInformation(String accessToken, String refreshToken) {
    return new JwtInformation(
        userDto,
        accessToken,
        refreshToken,
        Instant.now().plusSeconds(600),
        Instant.now().plusSeconds(3600)
    );
  }
}
