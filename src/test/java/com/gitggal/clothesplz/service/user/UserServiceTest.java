package com.gitggal.clothesplz.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;

import com.gitggal.clothesplz.dto.user.UserCreateRequest;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.user.UserRole;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.mapper.user.UserMapper;
import com.gitggal.clothesplz.repository.profile.ProfileRepository;
import com.gitggal.clothesplz.repository.user.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 비즈니스 로직 테스트")
class UserServiceTest {

  @InjectMocks
  private UserServiceImpl userService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ProfileRepository profileRepository;

  @Mock
  private UserMapper userMapper;

  @Mock
  private PasswordEncoder passwordEncoder;

  private UserCreateRequest request;
  private UserDto responseDto;
  private String encodedPassword;

  @BeforeEach
  void setUp() {
    request = new UserCreateRequest("홍길동", "test@test.com", "rawPassword");
    encodedPassword = "encodedPassword";
    responseDto = new UserDto(
        UUID.randomUUID(),
        Instant.now(),
        "test@test.com",
        "홍길동",
        UserRole.USER,
        false
    );
  }

  @Nested
  @DisplayName("회원가입")
  class CreateUser {

    @Test
    @DisplayName("성공 - 유효한 정보로 가입하면 유저 정보가 반환된다")
    void success_createUser() {
      // given
      given(passwordEncoder.encode(request.password())).willReturn(encodedPassword);
      given(userRepository.existsByEmail(request.email())).willReturn(false);
      given(userRepository.save(any(User.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      given(profileRepository.save(any(Profile.class)))
          .willAnswer(invocation -> invocation.getArgument(0));
      given(userMapper.toDto(any(User.class))).willReturn(responseDto);

      // when
      UserDto result = userService.create(request);

      // then
      assertThat(result.email()).isEqualTo(request.email());
      assertThat(result.name()).isEqualTo(request.name());

      verify(passwordEncoder).encode(anyString());
      verify(userRepository).save(any(User.class));
      verify(userMapper).toDto(any(User.class));
    }

    @Test
    @DisplayName("실패 - 이미 존재하는 이메일이면 예외가 발생한다")
    void fail_duplicateEmail() {
      // given
      given(userRepository.existsByEmail(request.email())).willReturn(true);

      // when & then
      assertThatThrownBy(() -> userService.create(request))
          .isInstanceOf(BusinessException.class);

      verify(userRepository, never()).save(any(User.class));
      verify(passwordEncoder, never()).encode(anyString());
    }
  }
}