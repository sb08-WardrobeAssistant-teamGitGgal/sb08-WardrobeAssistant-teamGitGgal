package com.gitggal.clothesplz.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.user.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtInformationTest {

  @Test
  @DisplayName("JWT 정보를 새 토큰으로 교체, 만료 여부를 확인")
  void rotateAndCheckExpiration() {
    UserDto userDto = new UserDto(
        UUID.randomUUID(),
        Instant.now(),
        "test@test.com",
        "홍길동",
        UserRole.USER,
        false
    );
    JwtInformation expiredJwtInformation = new JwtInformation(
        userDto,
        "old-access-token",
        "old-refresh-token",
        Instant.now().minusSeconds(1),
        Instant.now().minusSeconds(1)
    );

    JwtInformation rotated = expiredJwtInformation.rotate(
        "new-access-token",
        "new-refresh-token",
        Instant.now().plusSeconds(600),
        Instant.now().plusSeconds(3600)
    );

    assertThat(expiredJwtInformation.isAccessTokenExpired()).isTrue();
    assertThat(expiredJwtInformation.isRefreshTokenExpired()).isTrue();
    assertThat(rotated.accessToken()).isEqualTo("new-access-token");
    assertThat(rotated.refreshToken()).isEqualTo("new-refresh-token");
    assertThat(rotated.isAccessTokenExpired()).isFalse();
    assertThat(rotated.isRefreshTokenExpired()).isFalse();
  }
}
