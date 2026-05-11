package com.gitggal.clothesplz.controller.profile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.config.TestSecurityConfig;
import com.gitggal.clothesplz.dto.profile.common.WeatherAPILocation;
import com.gitggal.clothesplz.dto.profile.request.ProfileUpdateRequest;
import com.gitggal.clothesplz.dto.profile.response.ProfileDto;
import com.gitggal.clothesplz.entity.profile.Gender;
import com.gitggal.clothesplz.exception.GlobalExceptionHandler;
import com.gitggal.clothesplz.security.jwt.JwtAuthenticationFilter;
import com.gitggal.clothesplz.service.image.ImageUploader;
import com.gitggal.clothesplz.service.profile.ProfileService;
import com.gitggal.clothesplz.service.weather.WeatherApiService;
import java.time.LocalDate;
import java.util.List;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import({
    GlobalExceptionHandler.class,
    TestSecurityConfig.class
})
@WebMvcTest(
    controllers = ProfileController.class,
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class
        )
    }
)
@DisplayName("Profile Controller 테스트")
class ProfileControllerTest {

  @MockitoBean
  private ProfileService profileService;

  @MockitoBean
  private ImageUploader imageUploader;

  @MockitoBean
  private WeatherApiService weatherApiService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private UUID userId;
  private ProfileDto profileDto;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
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
    @DisplayName("성공 - 프로필 조회 시 200과 프로필 정보를 반환한다")
    void getProfile_returns200WithProfileData() throws Exception {
      given(profileService.getProfile(userId)).willReturn(profileDto);

      mockMvc.perform(get("/api/users/{userId}/profiles", userId))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.userId").value(userId.toString()))
          .andExpect(jsonPath("$.name").value("홍길동"))
          .andExpect(jsonPath("$.gender").value("MALE"))
          .andExpect(jsonPath("$.temperatureSensitivity").value(3));
    }
  }

  @Nested
  @DisplayName("프로필 수정 관련 테스트")
  class UpdateProfileTests {

    @Test
    @DisplayName("성공 - 이미지 없이 수정 시 200과 수정된 프로필을 반환한다")
    void updateProfile_withoutImage_returns200() throws Exception {
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
              .with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    @DisplayName("성공 - 이미지 포함 수정 시 200과 수정된 프로필을 반환한다")
    void updateProfile_withImage_returns200() throws Exception {
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
              .with(csrf()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    @DisplayName("실패 - name이 20자 초과이면 400을 반환한다")
    void updateProfile_nameTooLong_returns400() throws Exception {
      ProfileUpdateRequest request = new ProfileUpdateRequest(
          "가".repeat(21), null, null, null, null
      );

      mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/{userId}/profiles", userId)
              .file(toRequestPart(request))
              .with(csrf()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - temperatureSensitivity가 1 미만이면 400을 반환한다")
    void updateProfile_temperatureSensitivityTooLow_returns400() throws Exception {
      ProfileUpdateRequest request = new ProfileUpdateRequest(null, null, null, null, 0);

      mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/{userId}/profiles", userId)
              .file(toRequestPart(request))
              .with(csrf()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - temperatureSensitivity가 5 초과이면 400을 반환한다")
    void updateProfile_temperatureSensitivityTooHigh_returns400() throws Exception {
      ProfileUpdateRequest request = new ProfileUpdateRequest(null, null, null, null, 6);

      mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/{userId}/profiles", userId)
              .file(toRequestPart(request))
              .with(csrf()))
          .andExpect(status().isBadRequest());
    }
  }
}
