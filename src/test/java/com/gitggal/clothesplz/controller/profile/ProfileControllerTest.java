package com.gitggal.clothesplz.controller.profile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.dto.profile.common.WeatherAPILocation;
import com.gitggal.clothesplz.dto.profile.request.ProfileUpdateRequest;
import com.gitggal.clothesplz.dto.profile.response.ProfileDto;
import com.gitggal.clothesplz.entity.profile.Gender;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.user.UserRole;
import com.gitggal.clothesplz.repository.user.UserRepository;
import com.gitggal.clothesplz.service.image.ImageUploader;
import com.gitggal.clothesplz.service.profile.ProfileService;
import com.gitggal.clothesplz.service.weather.WeatherApiService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Profile Controller 테스트")
class ProfileControllerTest {

  @MockitoBean
  private Flyway flyway;

  @MockitoBean
  private ImageUploader imageUploader;

  @MockitoBean
  private ProfileService profileService;

  @MockitoBean
  private WeatherApiService weatherApiService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private static final String PASSWORD = "password123!";
  private static final String USER_EMAIL = "user@test.com";
  private static final String OTHER_EMAIL = "other@test.com";
  private static final String ADMIN_EMAIL = "admin@test.com";

  private UUID userId;
  private ProfileDto profileDto;

  @BeforeEach
  void setUp() {
    User user = userRepository.save(new User("홍길동", USER_EMAIL, passwordEncoder.encode(PASSWORD)));
    userRepository.save(new User("다른사용자", OTHER_EMAIL, passwordEncoder.encode(PASSWORD)));

    User admin = new User("어드민", ADMIN_EMAIL, passwordEncoder.encode(PASSWORD));
    ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN); // 관리자 Role 강제 주입
    userRepository.save(admin);

    userId = user.getId();
    profileDto = new ProfileDto(
        userId,
        "홍길동",
        Gender.MALE,
        LocalDate.of(1995, 5, 10),
        WeatherAPILocation.of(37.5, 127.0, 60, 127, List.of("서울특별시", "중구")),
        3,
        "http://localhost:8080/files/uuid.png"
    );
  }

  // 로그인 -> ACCESS TOKE 발급
  private String getAccessToken(String email) throws Exception {
    String body = mockMvc.perform(post("/api/auth/sign-in")
            .with(csrf())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("username", email)
            .param("password", PASSWORD))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    return objectMapper.readTree(body).get("accessToken").asText();
  }

  private MockMultipartFile toRequestPart(ProfileUpdateRequest request) throws Exception {
    return new MockMultipartFile(
        "request",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsBytes(request)
    );
  }

  @Nested
  @DisplayName("프로필 조회 관련 테스트")
  class GetProfileTests {

    @Test
    @DisplayName("성공 - 본인 조회 시 200 반환")
    void getProfile_withOwnToken_returns200() throws Exception {
      // given
      String token = getAccessToken(USER_EMAIL);
      given(profileService.getProfile(userId)).willReturn(profileDto);

      mockMvc.perform(get("/api/users/{userId}/profiles", userId)
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.userId").value(userId.toString()))
          .andExpect(jsonPath("$.name").value("홍길동"))
          .andExpect(jsonPath("$.gender").value("MALE"))
          .andExpect(jsonPath("$.temperatureSensitivity").value(3));
    }

    @Test
    @DisplayName("성공 - 어드민 조회 시 200 반환")
    void getProfile_withAdminToken_returns200() throws Exception {
      String token = getAccessToken(ADMIN_EMAIL);
      given(profileService.getProfile(userId)).willReturn(profileDto);

      mockMvc.perform(get("/api/users/{userId}/profiles", userId)
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("실패 - 타인 조회 시 403 반환")
    void getProfile_withOtherUsersToken_returns403() throws Exception {
      String token = getAccessToken(OTHER_EMAIL);

      mockMvc.perform(get("/api/users/{userId}/profiles", userId)
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("실패 - 토큰 없이 접근 시 401 반환")
    void getProfile_withoutToken_returns401() throws Exception {
      mockMvc.perform(get("/api/users/{userId}/profiles", userId))
          .andExpect(status().isFound())
          .andExpect(redirectedUrl("http://localhost/login"));
    }
  }

  @Nested
  @DisplayName("프로필 수정 관련 테스트")
  class UpdateProfileTests {

    @Test
    @DisplayName("성공 - 이미지 없이 수정 시 200 반환")
    void updateProfile_withoutImage_returns200() throws Exception {
      String token = getAccessToken(USER_EMAIL);
      ProfileUpdateRequest request = new ProfileUpdateRequest(
          "수정된이름",
          Gender.FEMALE,
          LocalDate.of(1995, 5, 10),
          null,
          2
      );
      given(profileService.updateProfile(eq(userId), any(), isNull())).willReturn(profileDto);

      mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/{userId}/profiles", userId)
              .file(toRequestPart(request))
              .with(csrf())
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    @DisplayName("성공 - 이미지 포함 수정 시 200 반환")
    void updateProfile_withImage_returns200() throws Exception {
      String token = getAccessToken(USER_EMAIL);
      ProfileUpdateRequest request = new ProfileUpdateRequest(
          "수정된이름",
          Gender.FEMALE,
          LocalDate.of(1995, 5, 10),
          null,
          2
      );

      MockMultipartFile image = new MockMultipartFile(
          "image",
          "profile.jpg",
          MediaType.IMAGE_JPEG_VALUE,
          "image-data".getBytes()
      );
      given(profileService.updateProfile(eq(userId), any(), any())).willReturn(profileDto);

      mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/{userId}/profiles", userId)
              .file(toRequestPart(request))
              .file(image)
              .with(csrf())
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    @DisplayName("실패 - 타인 수정 시 403 반환")
    void updateProfile_withOtherUsersToken_returns403() throws Exception {
      String token = getAccessToken(OTHER_EMAIL);
      ProfileUpdateRequest request = new ProfileUpdateRequest(null, null, null, null, null);

      mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/{userId}/profiles", userId)
              .file(toRequestPart(request))
              .with(csrf())
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("실패 - 토큰 없이 접근 시 401 반환")
    void updateProfile_withoutToken_returns401() throws Exception {
      ProfileUpdateRequest request = new ProfileUpdateRequest(null, null, null, null, null);

      mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/{userId}/profiles", userId)
              .file(toRequestPart(request))
              .with(csrf()))
          .andExpect(status().isFound())
          .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @DisplayName("실패 - name이 20자 초과이면 400 반환")
    void updateProfile_nameTooLong_returns400() throws Exception {
      String token = getAccessToken(USER_EMAIL);
      ProfileUpdateRequest request = new ProfileUpdateRequest(
          "가".repeat(21), null, null, null, null
      );

      mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/{userId}/profiles", userId)
              .file(toRequestPart(request))
              .with(csrf())
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - temperatureSensitivity가 1 미만이면 400 반환")
    void updateProfile_temperatureSensitivityTooLow_returns400() throws Exception {
      String token = getAccessToken(USER_EMAIL);
      ProfileUpdateRequest request = new ProfileUpdateRequest(null, null, null, null, 0);

      mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/{userId}/profiles", userId)
              .file(toRequestPart(request))
              .with(csrf())
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - temperatureSensitivity가 5 초과이면 400 반환")
    void updateProfile_temperatureSensitivityTooHigh_returns400() throws Exception {
      String token = getAccessToken(USER_EMAIL);
      ProfileUpdateRequest request = new ProfileUpdateRequest(null, null, null, null, 6);

      mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/{userId}/profiles", userId)
              .file(toRequestPart(request))
              .with(csrf())
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isBadRequest());
    }
  }
}
