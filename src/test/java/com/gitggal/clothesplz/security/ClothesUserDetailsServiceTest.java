package com.gitggal.clothesplz.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.user.UserRole;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.mapper.user.UserMapper;
import com.gitggal.clothesplz.repository.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class ClothesUserDetailsServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  private ClothesUserDetailsService userDetailsService;
  private User user;
  private UserDto userDto;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userDetailsService = new ClothesUserDetailsService(userRepository, userMapper);
    userId = UUID.randomUUID();
    user = new User("tester", "test@example.com", "encoded-password");
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
  @DisplayName("사용자를 조회 - 이메일")
  void loadUserByUsername() {
    when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
    when(userMapper.toDto(user)).thenReturn(userDto);

    UserDetails userDetails = userDetailsService.loadUserByUsername("test@test.com");

    assertThat(userDetails).isInstanceOf(ClothesUserDetails.class);
    assertThat(userDetails.getPassword()).isEqualTo("encoded-password");
  }

  @Test
  @DisplayName("사용자를 조회 - userId")
  void loadUserById() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userMapper.toDto(user)).thenReturn(userDto);

    UserDetails userDetails = userDetailsService.loadUserById(userId);

    assertThat(userDetails).isInstanceOf(ClothesUserDetails.class);
    assertThat(((ClothesUserDetails) userDetails).getUserDto()).isEqualTo(userDto);
  }

  @Test
  @DisplayName("사용자 조회 실패")
  void loadUserByUsername_UserNotFound() {
    when(userRepository.findByEmail("not@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userDetailsService.loadUserByUsername("not@example.com"))
        .isInstanceOf(BusinessException.class);
  }
}
