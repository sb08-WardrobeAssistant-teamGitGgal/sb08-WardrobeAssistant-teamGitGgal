package com.gitggal.clothesplz.controller.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.dto.user.UserCreateRequest;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.user.UserRole;
import com.gitggal.clothesplz.exception.GlobalExceptionHandler;
import com.gitggal.clothesplz.service.user.UserService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import(GlobalExceptionHandler.class)
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
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
        UUID.randomUUID(),
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
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(userCreateRequest)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.name").value("GitGit"))
          .andExpect(jsonPath("$.email").value("Git@git.git"));

    }

    @Test
    @DisplayName("회원가입 실패 - 유효성 검사 실패 시 400 Bad Request를 반환한다")
    void create_User_Validation_Fail() throws Exception {
      // given
      UserCreateRequest invalidRequest = new UserCreateRequest(null, "Git@git.git", "git1234!");

      // when & then
      mockMvc.perform(post("/api/users")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(invalidRequest)))
          .andDo(print())
          .andExpect(status().isBadRequest());
    }
  }

}
