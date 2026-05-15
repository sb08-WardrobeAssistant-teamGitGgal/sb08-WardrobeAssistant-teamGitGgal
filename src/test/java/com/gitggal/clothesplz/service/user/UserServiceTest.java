package com.gitggal.clothesplz.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.times;

import com.gitggal.clothesplz.dto.user.ChangePasswordRequest;
import com.gitggal.clothesplz.dto.user.UserCreateRequest;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.dto.user.UserDtoCursorRequest;
import com.gitggal.clothesplz.dto.user.UserDtoCursorResponse;
import com.gitggal.clothesplz.dto.user.UserRoleUpdateRequest;
import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.user.UserRole;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.mapper.user.UserMapper;
import com.gitggal.clothesplz.repository.profile.ProfileRepository;
import com.gitggal.clothesplz.repository.user.UserRepository;
import com.gitggal.clothesplz.security.jwt.JwtRegistry;
import java.time.Instant;
import java.util.List;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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

  @Mock
  private JwtRegistry jwtRegistry;

  private UserCreateRequest request;
  private UserDto userDto;
  private String encodedPassword;
  private User user;
  private UUID userId;

  @BeforeEach
  void setUp() {
    request = new UserCreateRequest("홍길동", "test@test.com", "rawPassword");
    encodedPassword = "encodedPassword";
    userDto = new UserDto(
        userId,
        Instant.now(),
        "test@test.com",
        "홍길동",
        UserRole.USER,
        false
    );
    user = new User(
        "홍길동",
        "test@test.com",
        "oldPassword");
    userId = UUID.randomUUID();
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
      given(userMapper.toDto(any(User.class))).willReturn(userDto);

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

  @Nested
  @DisplayName("비밀번호 변경")
  class updatePassword {

    @Test
    @DisplayName("성공 - 임시 비밀번호가 있으면 제거")
    void updatePassword_success_clearTempPassword() {
      // given
      ChangePasswordRequest request =
          new ChangePasswordRequest("newPassword123!");

      user.updateTempPassword("tempPassword");

      given(userRepository.findById(userId))
          .willReturn(Optional.of(user));

      given(passwordEncoder.encode(request.password()))
          .willReturn("encodedPassword");

      // when
      userService.updatePassword(userId, request);

      // then
      assertThat(user.getPassword()).isEqualTo("encodedPassword");
      assertThat(user.getTempPassword()).isNull();

      verify(userRepository).findById(userId);
      verify(passwordEncoder).encode(request.password());
    }

    @Test
    @DisplayName("성공 - 임시 비밀번호가 없는 경우")
    void updatePassword_success_withoutTempPassword() {
      // given
      ChangePasswordRequest request =
          new ChangePasswordRequest("newPassword123!");

      given(userRepository.findById(userId))
          .willReturn(Optional.of(user));

      given(passwordEncoder.encode(request.password()))
          .willReturn("encodedPassword");

      // when
      userService.updatePassword(userId, request);

      // then
      assertThat(user.getPassword()).isEqualTo("encodedPassword");
      assertThat(user.getTempPassword()).isNull();

      verify(userRepository).findById(userId);
      verify(passwordEncoder).encode(request.password());
    }

    @Test
    @DisplayName("실패 - 사용자를 찾을 수 없음")
    void updatePassword_fail_userNotFound() {
      // given
      UUID userId = UUID.randomUUID();

      ChangePasswordRequest request =
          new ChangePasswordRequest("newPassword123!");

      given(userRepository.findById(userId))
          .willReturn(Optional.empty());

      // when & then
      BusinessException exception = assertThrows(
          BusinessException.class,
          () -> userService.updatePassword(userId, request));

      assertThat(exception.getErrorCode())
          .isEqualTo(UserErrorCode.USER_NOT_FOUND);

      verify(userRepository).findById(userId);
      verify(passwordEncoder, never()).encode(anyString());
    }
  }

  @Nested
  @DisplayName("역할 변경")
  class UpdateRole {

    @Test
    @DisplayName("성공")
    void updateRole_success_userToAdmin() {
      // given
      UserRoleUpdateRequest request = new UserRoleUpdateRequest(UserRole.ADMIN);

      UserDto updatedDto = new UserDto(
          userId,
          Instant.now(),
          "test@test.com",
          "TestUser",
          UserRole.ADMIN,
          false
      );

      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(userMapper.toDto(user)).willReturn(updatedDto);

      // when
      userService.updateRole(userId, request);

      // then
      assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
      verify(userMapper).toDto(user);
      verify(jwtRegistry).invalidateJwtInformationByUserId(userId);
    }

    @Test
    @DisplayName("실패 - 이미 같은 역할")
    void updateRole_skip_sameRole() {
      // given
      UserRoleUpdateRequest request = new UserRoleUpdateRequest(UserRole.USER);

      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(userMapper.toDto(user)).willReturn(userDto);

      // when
      UserDto result = userService.updateRole(userId, request);

      // then
      assertThat(result.role()).isEqualTo(UserRole.USER);
      verify(jwtRegistry, never()).invalidateJwtInformationByUserId(any());
    }

    @Test
    @DisplayName("실패 - 사용자를 찾을 수 없음")
    void updateRole_fail_userNotFound() {
      // given
      UserRoleUpdateRequest request = new UserRoleUpdateRequest(UserRole.ADMIN);
      given(userRepository.findById(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userService.updateRole(userId, request))
          .isInstanceOf(BusinessException.class);

      verify(jwtRegistry, never()).invalidateJwtInformationByUserId(any());
    }
  }

  @Nested
  @DisplayName("계정 목록 조회")
  class findAll {

    @Test
    @DisplayName("성공 - 다음 페이지 없음")
    void findAll_success() {

      // given
      UserDtoCursorRequest request = new UserDtoCursorRequest(
          null,
          null,
          5,
          "email",
          "ASCENDING",
          null,
          null,
          null
      );

      given(userRepository.getAllUsers(request)).willReturn(List.of(user));
      given(userMapper.toDto(user)).willReturn(userDto);

      // when
      UserDtoCursorResponse response = userService.findAll(request);

      // then
      assertThat(response).isNotNull();
      assertThat(response.data()).hasSize(1);
      assertThat(response.data().get(0)).isEqualTo(userDto);
      assertThat(response.nextCursor()).isNull();
      assertThat(response.hasNext()).isFalse();
      assertThat(response.totalCount()).isEqualTo(1);
      assertThat(response.sortBy()).isEqualTo("email");
      assertThat(response.sortDirection()).isEqualTo("ASCENDING");

      verify(userRepository, times(1)).getAllUsers(request);
    }

    @Test
    @DisplayName("성공 - 다음 페이지 있음")
    void findAll_nextPage_success() {

      // given
      UserDtoCursorRequest request = new UserDtoCursorRequest(
          null,
          null,
          2,
          "email",
          "ASCENDING",
          null,
          null,
          null
      );

      User user1 = new User("a", "a@com", "pw");
      User user2 = new User("b", "b@com", "pw");
      ReflectionTestUtils.setField(user1, "id", UUID.randomUUID());

      UserDto userDto1 = new UserDto(user1.getId(), Instant.now(), user1.getEmail(),
          user1.getName(), UserRole.USER, false);

      given(userRepository.getAllUsers(request)).willReturn(List.of(user, user1, user2));
      given(userMapper.toDto(user)).willReturn(userDto);
      given(userMapper.toDto(user1)).willReturn(userDto1);

      // when
      UserDtoCursorResponse response = userService.findAll(request);

      // then
      assertThat(response).isNotNull();
      assertThat(response.data()).hasSize(2);
      assertThat(response.data().get(0)).isEqualTo(userDto);
      assertThat(response.nextCursor()).isNotNull();
      assertThat(response.nextIdAfter()).isNotNull();
      assertThat(response.hasNext()).isTrue();
      assertThat(response.totalCount()).isEqualTo(2);
      assertThat(response.sortBy()).isEqualTo("email");
      assertThat(response.sortDirection()).isEqualTo("ASCENDING");

      verify(userRepository, times(1)).getAllUsers(request);
    }
  }
}