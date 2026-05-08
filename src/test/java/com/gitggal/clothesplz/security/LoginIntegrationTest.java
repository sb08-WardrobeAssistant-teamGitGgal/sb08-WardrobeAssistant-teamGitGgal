package com.gitggal.clothesplz.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.repository.user.UserRepository;
import com.gitggal.clothesplz.service.image.ImageUploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LoginIntegrationTest {

  @MockitoBean
  private ImageUploader imageUploader;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private ObjectMapper objectMapper;

  private static final String TEST_EMAIL = "test@test.com";
  private static final String TEST_PASSWORD = "password123!";
  private static final String TEST_NAME = "tester";

  @BeforeEach
  void setUp() {
    User user = new User(
        TEST_NAME,
        TEST_EMAIL,
        passwordEncoder.encode(TEST_PASSWORD)
    );

    userRepository.save(user);
  }

  @Test
  @DisplayName("로그인 성공")
  void loginSuccess() throws Exception {
    mockMvc.perform(post("/api/auth/sign-in")
            .with(csrf())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("email", TEST_EMAIL)
            .param("password", TEST_PASSWORD))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userDto.email").value(TEST_EMAIL))
        .andExpect(jsonPath("$.userDto.name").value(TEST_NAME))
        .andExpect(jsonPath("$.accessToken").exists())
        .andExpect(cookie().exists("REFRESH_TOKEN"))
        .andExpect(cookie().httpOnly("REFRESH_TOKEN", true));
  }

  @Test
  @DisplayName("로그인 실패 - 잘못된 비밀번호")
  void loginFailure_WrongPassword() throws Exception {
    mockMvc.perform(post("/api/auth/sign-in")
            .with(csrf())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("email", TEST_EMAIL)
            .param("password", "wrong"))
        .andDo(print())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.exceptionName").value("AUTHENTICATION_FAILED"));
  }

  @Test
  @DisplayName("로그인 실패 - 존재하지 않는 이메일")
  void loginFailure_UserNotFound() throws Exception {
    mockMvc.perform(post("/api/auth/sign-in")
            .with(csrf())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("email", "not@example.com")
            .param("password", TEST_PASSWORD))
        .andDo(print())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.exceptionName").value("AUTHENTICATION_FAILED"));
  }

  @Test
  @DisplayName("로그인 실패 - CSRF 토큰 없음")
  void loginFailure_NoCsrfToken() throws Exception {
    mockMvc.perform(post("/api/auth/sign-in")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("email", TEST_EMAIL)
            .param("password", TEST_PASSWORD))
        .andDo(print())
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("로그인 성공 후 기존 세션 무효화")
  void loginSuccess_InvalidatesPreviousSession() throws Exception {

    String firstAccessToken = mockMvc.perform(post("/api/auth/sign-in")
            .with(csrf())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("email", TEST_EMAIL)
            .param("password", TEST_PASSWORD))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    mockMvc.perform(post("/api/auth/sign-in")
            .with(csrf())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("email", TEST_EMAIL)
            .param("password", TEST_PASSWORD))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").exists());

  }

  @Test
  @DisplayName("로그인 성공 - Refresh Token 쿠키 확인")
  void loginSuccess_RefreshTokenCookie() throws Exception {
    mockMvc.perform(post("/api/auth/sign-in")
            .with(csrf())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("email", TEST_EMAIL)
            .param("password", TEST_PASSWORD))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(cookie().exists("REFRESH_TOKEN"))
        .andExpect(cookie().httpOnly("REFRESH_TOKEN", true))
        .andExpect(cookie().secure("REFRESH_TOKEN", true))
        .andExpect(cookie().path("REFRESH_TOKEN", "/"));
  }
}