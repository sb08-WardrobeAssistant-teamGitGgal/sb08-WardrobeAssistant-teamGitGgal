package com.gitggal.clothesplz.controller.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.config.TestSecurityConfig;
import com.gitggal.clothesplz.dto.user.ChangePasswordRequest;
import com.gitggal.clothesplz.dto.user.UserCreateRequest;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.dto.user.UserRoleUpdateRequest;
import com.gitggal.clothesplz.entity.user.UserRole;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.GlobalExceptionHandler;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import com.gitggal.clothesplz.security.jwt.JwtAuthenticationFilter;
import com.gitggal.clothesplz.service.user.UserService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import({
    GlobalExceptionHandler.class,
    TestSecurityConfig.class
})
@WebMvcTest(
    controllers = UserController.class,
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class
        )
    }
)
@DisplayName("UserController Test")
public class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

  private UserCreateRequest userCreateRequest;
  private UserDto userDto;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();

    userCreateRequest = new UserCreateRequest(
        "GitGit",
        "Git@git.git",
        "git1234!"
    );
    userDto = new UserDto(
        userId,
        Instant.now(),
        "Git@git.git",
        "GitGit",
        UserRole.USER,
        false
    );
  }

  @Nested
  @DisplayName("회원가입")
  class createUser {

    @Test
    @DisplayName("회원가입 성공")
    void success_createUser() throws Exception {
      //given
      given(userService.create(any(UserCreateRequest.class))).willReturn(userDto);

      //when & then
      mockMvc.perform(post("/api/users")
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(userCreateRequest)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.name").value("GitGit"))
          .andExpect(jsonPath("$.email").value("Git@git.git"));

    }

    @Test
    @DisplayName("회원가입 실패")
    void create_User_Validation_Fail() throws Exception {
      // given
      UserCreateRequest invalidRequest = new UserCreateRequest(null, "Git@git.git", "git1234!");

      // when & then
      mockMvc.perform(post("/api/users")
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(invalidRequest)))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("비밀번호 변경")
  class updatePassword {

    @Test
    @DisplayName("비밀번호 변경 성공")
    void success_updatePassword() throws Exception {
      // given
      ChangePasswordRequest request = new ChangePasswordRequest("newPassword123!");
      ClothesUserDetails principal = new ClothesUserDetails(
          userDto,
          "encodedPassword"
      );
      // when & then
      mockMvc.perform(patch("/api/users/{userId}/password", userId)
              .with(user(principal))
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNoContent());

      verify(userService).updatePassword(eq(userId), any(ChangePasswordRequest.class));
    }

    @Test
    @DisplayName("비밀번호 변경 실패")
    void updatePassword_validation_fail() throws Exception {
      // given
      ChangePasswordRequest invalidRequest = new ChangePasswordRequest("");
      ClothesUserDetails principal = new ClothesUserDetails(
          userDto,
          "encodedPassword"
      );
      // when & then
      mockMvc.perform(patch("/api/users/{userId}/password", userId)
              .with(user(principal))
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(invalidRequest)))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("역할 변경")
  class UpdateRole {

    @Test
    @DisplayName("역할 변경 성공")
    void updateRole_success_withAdminRole() throws Exception {
      // given
      UserRoleUpdateRequest request = new UserRoleUpdateRequest(UserRole.ADMIN);
      UserDto adminDto = new UserDto(
          userId,
          Instant.now(),
          "Git@git.git",
          "GitGit",
          UserRole.ADMIN,
          false
      );

      given(userService.updateRole(eq(userId), any(UserRoleUpdateRequest.class)))
          .willReturn(adminDto);

      // when & then
      mockMvc.perform(patch("/api/users/{userId}/role", userId)
              .with(user("admin@test.com").roles("ADMIN"))
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(userId.toString()))
          .andExpect(jsonPath("$.role").value("ADMIN"));

      verify(userService).updateRole(eq(userId), any(UserRoleUpdateRequest.class));
    }

    @Test
    @DisplayName("역할 변경 실패 - 사용자를 찾을 수 없음")
    void updateRole_fail_userNotFound() throws Exception {
      // given
      UserRoleUpdateRequest request = new UserRoleUpdateRequest(UserRole.ADMIN);

      given(userService.updateRole(eq(userId), any(UserRoleUpdateRequest.class)))
          .willThrow(new BusinessException(UserErrorCode.USER_NOT_FOUND));

      // when & then
      mockMvc.perform(patch("/api/users/{userId}/role", userId)
              .with(user("admin@test.com").roles("ADMIN"))
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andDo(print())
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.exceptionName").value(UserErrorCode.USER_NOT_FOUND.name()));

      verify(userService).updateRole(eq(userId), any(UserRoleUpdateRequest.class));
    }

    @Test
    @DisplayName("역할 변경 실패 - 유효성 검사 실패")
    void updateRole_fail_validationFailed() throws Exception {
      // given
      String invalidRequest = "{}";

      // when & then
      mockMvc.perform(patch("/api/users/{userId}/role", userId)
              .with(user("admin@test.com").roles("ADMIN"))
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(invalidRequest))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }
  }

}