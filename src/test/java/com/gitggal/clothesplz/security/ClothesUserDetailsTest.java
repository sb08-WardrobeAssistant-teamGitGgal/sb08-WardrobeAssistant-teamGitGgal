package com.gitggal.clothesplz.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.user.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClothesUserDetailsTest {

  @Test
  @DisplayName("username, password, 권한 반환")
  void userDetails() {
    UserDto userDto = new UserDto(
        UUID.randomUUID(),
        Instant.now(),
        "test@test.com",
        "홍길동",
        UserRole.USER,
        false
    );

    ClothesUserDetails userDetails = new ClothesUserDetails(userDto, "encoded-password");

    assertThat(userDetails.getUserDto()).isEqualTo(userDto);
    assertThat(userDetails.getUsername()).isEqualTo("홍길동");
    assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
    assertThat(userDetails.getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_USER");
  }
}
