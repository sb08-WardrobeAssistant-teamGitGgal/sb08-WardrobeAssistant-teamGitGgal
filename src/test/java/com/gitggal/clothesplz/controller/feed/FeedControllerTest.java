package com.gitggal.clothesplz.controller.feed;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.dto.feed.FeedCreateRequest;
import com.gitggal.clothesplz.dto.feed.FeedDto;
import com.gitggal.clothesplz.dto.feed.FeedUpdateRequest;
import com.gitggal.clothesplz.dto.user.AuthorDto;
import com.gitggal.clothesplz.dto.weather.PrecipitationDto;
import com.gitggal.clothesplz.dto.weather.TemperatureDto;
import com.gitggal.clothesplz.dto.weather.WeatherSummaryDto;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.SkyStatus;
import com.gitggal.clothesplz.service.feed.FeedService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FeedController.class)
@AutoConfigureMockMvc(addFilters = false) // 테스트를 위해 security 끄기
@DisplayName("피드 컨트롤러 테스트")
public class FeedControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private FeedService feedService;

  private UUID weatherId;
  private UUID authorId;
  private UUID feedId;
  private FeedDto feedDto;

  @BeforeEach
  void setUp() {
    authorId = UUID.randomUUID();
    weatherId = UUID.randomUUID();
    feedId = UUID.randomUUID();
    feedDto = new FeedDto(
        UUID.randomUUID(), Instant.now(), Instant.now(),
        new AuthorDto(authorId, "피드 작성자", "profileUrl"),
        new WeatherSummaryDto(
            weatherId,
            SkyStatus.CLEAR,
            new PrecipitationDto(PrecipitationType.NONE, 0.1, 0.1),
            new TemperatureDto(0.1, 0.1, 0.1, 0.1)),
        List.of(), "피드 내용 테스트", 0L, 0, false
    );
  }

  @Nested
  @DisplayName("피드 생성 관련 테스트")
  class CreateFeedTests {

    @Test
    @DisplayName("성공 - 201 반환")
    void createFeed_success() throws Exception {
      FeedCreateRequest request = new FeedCreateRequest(
          authorId, weatherId, List.of(UUID.randomUUID()), "피드 내용 테스트");
      given(feedService.createFeed(any())).willReturn(feedDto);

      mockMvc.perform(post("/api/feeds")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.content").value("피드 내용 테스트"));
    }

    @Test
    @DisplayName("실패 - authorId 없으면 400 반환")
    void createFeed_missingAuthorId_returns400() throws Exception {
      FeedCreateRequest request = new FeedCreateRequest(
          null, weatherId, List.of(UUID.randomUUID()), "피드 내용 테스트");

      mockMvc.perform(post("/api/feeds")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - weatherId 없으면 400 반환")
    void createFeed_missingWeatherId_returns400() throws Exception {
      FeedCreateRequest request = new FeedCreateRequest(
          authorId, null, List.of(UUID.randomUUID()), "피드 내용 테스트");

      mockMvc.perform(post("/api/feeds")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - clothesIds 비어있으면 400 반환")
    void createFeed_emptyClothesIds_returns400() throws Exception {
      FeedCreateRequest request = new FeedCreateRequest(
          authorId, weatherId, List.of(), "피드 내용 테스트");

      mockMvc.perform(post("/api/feeds")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - content 비어있으면 400 반환")
    void createFeed_blankContent_returns400() throws Exception {
      FeedCreateRequest request = new FeedCreateRequest(
          authorId, weatherId, List.of(UUID.randomUUID()), "");

      mockMvc.perform(post("/api/feeds")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("피드 수정 관련 테스트")
  class UpdateFeedTests {

    @Test
    @DisplayName("성공 - 200 반환")
    void updateFeed_success() throws Exception {
      FeedUpdateRequest request = new FeedUpdateRequest("피드 수정");
      given(feedService.updateFeed(eq(feedId), any())).willReturn(feedDto);

      mockMvc.perform(patch("/api/feeds/{feedId}", feedId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("실패 - content 비어있으면 400 반환")
    void updateFeed_blankContent_returns400() throws Exception {
      FeedUpdateRequest request = new FeedUpdateRequest("");

      mockMvc.perform(patch("/api/feeds/{feedId}", feedId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }
}
