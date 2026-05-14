package com.gitggal.clothesplz.controller.clothes;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gitggal.clothesplz.config.TestSecurityConfig;
import com.gitggal.clothesplz.dto.clothes.ClothesDto;
import com.gitggal.clothesplz.dto.clothes.RecommendationDto;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import com.gitggal.clothesplz.entity.user.UserRole;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.GlobalExceptionHandler;
import com.gitggal.clothesplz.exception.code.WeatherErrorCode;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import com.gitggal.clothesplz.security.jwt.JwtAuthenticationFilter;
import com.gitggal.clothesplz.service.clothes.RecommendationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("Recommendations Controller 테스트")
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@WebMvcTest(
    controllers = RecommendationsController.class,
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class
        )
    }
)
class RecommendationsControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private RecommendationService recommendationService;

  @Test
  @DisplayName("성공 - 추천 조회 시 200과 추천 목록을 반환한다")
  void getRecommendations_returns200() throws Exception {
    UUID weatherId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    ClothesUserDetails userDetails = createUserDetails(userId);
    UserDto userDto = userDetails.getUserDto();

    RecommendationDto response = new RecommendationDto(
        weatherId.toString(),
        userId.toString(),
        List.of(new ClothesDto(
            UUID.randomUUID(),
            userId,
            "반팔 티셔츠",
            null,
            ClothesType.TOP,
            List.of()))
    );
    given(recommendationService.getRecommendations(weatherId.toString(), userDto))
        .willReturn(response);

    mockMvc.perform(get("/api/recommendations")
            .param("weatherId", weatherId.toString())
            .with(user(userDetails)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.weatherId").value(weatherId.toString()))
        .andExpect(jsonPath("$.userId").value(userId.toString()))
        .andExpect(jsonPath("$.clothes[0].name").value("반팔 티셔츠"))
        .andExpect(jsonPath("$.clothes[0].type").value("TOP"));

    verify(recommendationService).getRecommendations(weatherId.toString(), userDto);
  }

  @Test
  @DisplayName("실패 - weatherId 파라미터가 없으면 400을 반환한다")
  void getRecommendations_missingWeatherId_returns400() throws Exception {
    mockMvc.perform(get("/api/recommendations")
            .with(user(createUserDetails(UUID.randomUUID()))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.exceptionName").value("INVALID_INPUT"));
  }

  @Test
  @DisplayName("실패 - 존재하지 않는 날씨 ID면 404를 반환한다")
  void getRecommendations_weatherNotFound_returns404() throws Exception {
    UUID weatherId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    ClothesUserDetails userDetails = createUserDetails(userId);

    given(recommendationService.getRecommendations(weatherId.toString(), userDetails.getUserDto()))
        .willThrow(new BusinessException(WeatherErrorCode.WEATHER_NOT_FOUND));

    mockMvc.perform(get("/api/recommendations")
            .param("weatherId", weatherId.toString())
            .with(user(userDetails)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.exceptionName").value("WEATHER_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value(WeatherErrorCode.WEATHER_NOT_FOUND.getMessage()));
  }

  private ClothesUserDetails createUserDetails(UUID userId) {
    return new ClothesUserDetails(
        new UserDto(
            userId,
            Instant.now(),
            "user@test.com",
            "user",
            UserRole.USER,
            false
        ),
        "pw"
    );
  }
}
