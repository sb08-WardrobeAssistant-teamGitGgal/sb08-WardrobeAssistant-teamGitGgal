package com.gitggal.clothesplz.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

import com.gitggal.clothesplz.dto.user.ChangePasswordRequest;
import com.gitggal.clothesplz.dto.user.UserCreateRequest;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.dto.user.UserDtoCursorRequest;
import com.gitggal.clothesplz.dto.user.UserDtoCursorResponse;
import com.gitggal.clothesplz.dto.user.UserLockUpdateRequest;
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
@DisplayName("UserService Test")
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
    userId = UUID.randomUUID();
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
  }

  @Nested
  @DisplayName("회원가입")
  class CreateUser {

    @Test
    @DisplayName("성공")
    void createUser_success() {
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

      then(userRepository).should().save(any(User.class));
      then(profileRepository).should().save(any(Profile.class));
    }

    @Test
    @DisplayName("실패 - 이미 존재하는 이메일")
    void createUser_fail_duplicateEmail() {
      // given
      given(userRepository.existsByEmail(request.email())).willReturn(true);

      // when & then
      assertThatThrownBy(() -> userService.create(request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.DUPLICATE_EMAIL);

      then(userRepository).should(never()).save(any(User.class));
      then(passwordEncoder).should(never()).encode(anyString());
    }
  }

  @Nested
  @DisplayName("비밀번호 변경")
  class UpdatePassword {

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
      assertThatThrownBy(() -> userService.updatePassword(userId, request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);

      then(passwordEncoder).should(never()).encode(anyString());
    }
  }

  @Nested
  @DisplayName("역할 변경")
  class UpdateRole {

    @Test
    @DisplayName("성공")
    void updateRole_success() {
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

      then(jwtRegistry).should().invalidateJwtInformationByUserId(userId);
    }

    @Test
    @DisplayName("실패 - 이미 같은 역할")
    void updateRole_fail_sameRole() {
      // given
      UserRoleUpdateRequest request = new UserRoleUpdateRequest(UserRole.USER);

      given(userRepository.findById(userId)).willReturn(Optional.of(user));
      given(userMapper.toDto(user)).willReturn(userDto);

      // when
      UserDto result = userService.updateRole(userId, request);

      // then
      assertThat(result.role()).isEqualTo(UserRole.USER);

      then(jwtRegistry).should(never()).invalidateJwtInformationByUserId(any());
    }

    @Test
    @DisplayName("실패 - 사용자를 찾을 수 없음")
    void updateRole_fail_userNotFound() {
      // given
      UserRoleUpdateRequest request = new UserRoleUpdateRequest(UserRole.ADMIN);
      given(userRepository.findById(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userService.updateRole(userId, request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);

      then(jwtRegistry).should(never()).invalidateJwtInformationByUserId(any());
    }
  }

  @Nested
  @DisplayName("계정 목록 조회")
  class FindAll {

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
      given(userRepository.totalCount(request)).willReturn(1L);

      // when
      UserDtoCursorResponse response = userService.findAll(request);

      // then
      assertThat(response).isNotNull();
      assertThat(response.data()).hasSize(1);
      assertThat(response.nextCursor()).isNull();
      assertThat(response.hasNext()).isFalse();
      assertThat(response.totalCount()).isEqualTo(1);

      then(userRepository).should().getAllUsers(request);
    }

    @Test
    @DisplayName("성공 - 다음 페이지 있음")
    void findAll_success_nextPage() {

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
      given(userRepository.totalCount(request)).willReturn(3L);

      // when
      UserDtoCursorResponse response = userService.findAll(request);

      // then
      assertThat(response).isNotNull();
      assertThat(response.data()).hasSize(2);
      assertThat(response.nextCursor()).isNotNull();
      assertThat(response.nextIdAfter()).isNotNull();
      assertThat(response.hasNext()).isTrue();
      assertThat(response.totalCount()).isEqualTo(3);

      then(userRepository).should().getAllUsers(request);
    }
  }

  @Nested
  @DisplayName("계정 잠금 상태 변경")
  class UpdateLock {

    @Test
    @DisplayName("성공 - 잠금")
    void updateLock_success_lock() {

      // given
      UserLockUpdateRequest request = new UserLockUpdateRequest(true);
      UserDto userTrueDto = new UserDto(
          userId,
          Instant.now(),
          "Git@git.git",
          "GitGit",
          UserRole.USER,
          true
      );

      given(userRepository.findById(userId))
          .willReturn(Optional.of(user));
      given(userMapper.toDto(user)).willReturn(userTrueDto);

      // when
      userService.updateLock(userId, request);

      // then
      assertThat(user.isLocked()).isTrue();

      then(jwtRegistry).should().invalidateJwtInformationByUserId(userId);
    }

    @Test
    @DisplayName("성공 - 해제")
    void updateLock_success_unlock() {

      // given
      UserLockUpdateRequest request = new UserLockUpdateRequest(false);

      given(userRepository.findById(userId))
          .willReturn(Optional.of(user));
      given(userMapper.toDto(user)).willReturn(userDto);

      // when
      userService.updateLock(userId, request);

      // then
      assertThat(user.isLocked()).isFalse();

      then(jwtRegistry).should(never()).invalidateJwtInformationByUserId(any());
    }

    @Test
    @DisplayName("실패 - 사용자를 찾을 수 없음")
    void updateLock_fail_userNotFound() {

      // given
      UserLockUpdateRequest request = new UserLockUpdateRequest(true);
      given(userRepository.findById(userId)).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> userService.updateLock(userId, request))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);

      then(jwtRegistry).should(never()).invalidateJwtInformationByUserId(any());
    }
  }
}