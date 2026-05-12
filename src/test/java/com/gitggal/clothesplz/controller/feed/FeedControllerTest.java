package com.gitggal.clothesplz.controller.feed;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.config.TestSecurityConfig;
import com.gitggal.clothesplz.dto.feed.CommentCreateRequest;
import com.gitggal.clothesplz.dto.feed.CommentDto;
import com.gitggal.clothesplz.dto.feed.CommentDtoCursorResponse;
import com.gitggal.clothesplz.dto.feed.CommentPageRequest;
import com.gitggal.clothesplz.dto.feed.FeedCreateRequest;
import com.gitggal.clothesplz.dto.feed.FeedDto;
import com.gitggal.clothesplz.dto.feed.FeedUpdateRequest;
import com.gitggal.clothesplz.dto.user.AuthorDto;
import com.gitggal.clothesplz.dto.weather.PrecipitationDto;
import com.gitggal.clothesplz.dto.weather.TemperatureDto;
import com.gitggal.clothesplz.dto.weather.WeatherSummaryDto;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.SkyStatus;
import com.gitggal.clothesplz.exception.GlobalExceptionHandler;
import com.gitggal.clothesplz.security.jwt.JwtAuthenticationFilter;
import com.gitggal.clothesplz.service.feed.FeedService;
import java.time.Instant;
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
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import({
    GlobalExceptionHandler.class,
    TestSecurityConfig.class
})
@WebMvcTest(
    controllers = FeedController.class,
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class
        )
    }
)
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
  private UUID userId;
  private UUID feedId;
  private FeedDto feedDto;
  private CommentDto commentDto1;
  private CommentDto commentDto2;

  @BeforeEach
  void setUp() {
    authorId = UUID.randomUUID();
    weatherId = UUID.randomUUID();
    userId = UUID.randomUUID();
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

    commentDto1 = new CommentDto(
        UUID.randomUUID(), Instant.now(), feedId,
        new AuthorDto(authorId, "작성자1", "url1"), "댓글1"
    );

    commentDto2 = new CommentDto(
        UUID.randomUUID(), Instant.now(), feedId,
        new AuthorDto(authorId, "작성자2", "url2"), "댓글2"
    );
  }

  @Nested
  @DisplayName("피드 생성 관련 테스트")
  class CreateFeedTests {

    @Test
    @DisplayName("성공 - 201 반환")
    void createFeed_Success() throws Exception {
      FeedCreateRequest request = new FeedCreateRequest(
          authorId, weatherId, List.of(UUID.randomUUID()), "피드 내용 테스트");
      given(feedService.createFeed(any())).willReturn(feedDto);

      mockMvc.perform(post("/api/feeds")
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.content").value("피드 내용 테스트"));
    }

    @Test
    @DisplayName("실패 - authorId 없으면 400 반환")
    void createFeed_MissingAuthorId_Returns400() throws Exception {
      FeedCreateRequest request = new FeedCreateRequest(
          null, weatherId, List.of(UUID.randomUUID()), "피드 내용 테스트");

      mockMvc.perform(post("/api/feeds")
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - weatherId 없으면 400 반환")
    void createFeed_MissingWeatherId_Returns400() throws Exception {
      FeedCreateRequest request = new FeedCreateRequest(
          authorId, null, List.of(UUID.randomUUID()), "피드 내용 테스트");

      mockMvc.perform(post("/api/feeds")
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - clothesIds 비어있으면 400 반환")
    void createFeed_EmptyClothesIds_Returns400() throws Exception {
      FeedCreateRequest request = new FeedCreateRequest(
          authorId, weatherId, List.of(), "피드 내용 테스트");

      mockMvc.perform(post("/api/feeds")
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - content 비어있으면 400 반환")
    void createFeed_BlankContent_Returns400() throws Exception {
      FeedCreateRequest request = new FeedCreateRequest(
          authorId, weatherId, List.of(UUID.randomUUID()), "");

      mockMvc.perform(post("/api/feeds")
              .with(csrf())
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
    void updateFeed_Success() throws Exception {
      FeedUpdateRequest request = new FeedUpdateRequest("피드 수정");
      given(feedService.updateFeed(eq(feedId), any())).willReturn(feedDto);

      mockMvc.perform(patch("/api/feeds/{feedId}", feedId)
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("실패 - content 비어있으면 400 반환")
    void updateFeed_BlankContent_Returns400() throws Exception {
      FeedUpdateRequest request = new FeedUpdateRequest("");

      mockMvc.perform(patch("/api/feeds/{feedId}", feedId)
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("피드 삭제 관련 테스트")
  class DeleteFeedTests {

    @Test
    @DisplayName("성공 - 204 반환")
    void deleteFeed_Success() throws Exception {
      willDoNothing().given(feedService).deleteFeed(feedId);

      mockMvc.perform(delete("/api/feeds/{feedId}", feedId)
              .with(csrf()))
          .andExpect(status().isNoContent());
    }
  }

  @Nested
  @DisplayName("피드 좋아요 관련 테스트")
  class IncreaseLikeTests {

    @Test
    @DisplayName("성공 - 204 반환")
    void increaseLikeCount_Success() throws Exception {
      willDoNothing().given(feedService).increaseLikeCount(eq(feedId), eq(userId));

      mockMvc.perform(post("/api/feeds/{feedId}/like", feedId)
              .with(csrf())
              .param("userId", userId.toString()))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("실패 - userId 없으면 400 반환")
    void increaseLikeCount_MissingUserId_Returns400() throws Exception {
      mockMvc.perform(post("/api/feeds/{feedId}/like", feedId)
              .with(csrf()))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("피드 좋아요 취소 관련 테스트")
  class DecreaseLikeCountTests {

    @Test
    @DisplayName("성공 - 204 반환")
    void decreaseLikeCount_Success() throws Exception {
      willDoNothing().given(feedService).decreaseLikeCount(eq(feedId), eq(userId));

      mockMvc.perform(delete("/api/feeds/{feedId}/like", feedId)
              .with(csrf())
              .param("userId", userId.toString()))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("실패 - userId 없으면 400 반환")
    void decreaseLikeCount_MissingUserId_Returns400() throws Exception {
      mockMvc.perform(delete("/api/feeds/{feedId}/like", feedId)
              .with(csrf()))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("피드 댓글 생성 관련 테스트")
  class CreateCommentTests {

    @Test
    @DisplayName("성공 - 201 반환")
    void createComment_Success() throws Exception {
      CommentCreateRequest request = new CommentCreateRequest(feedId, authorId, "댓글 내용 테스트");

      given(feedService.createComment(eq(feedId), any())).willReturn(commentDto1);

      mockMvc.perform(post("/api/feeds/{feedId}/comments", feedId)
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.content").value("댓글 내용 테스트"));
    }

    @Test
    @DisplayName("실패 - feedId 없으면 400 반환")
    void createComment_MissingFeedId_Returns400() throws Exception {
      CommentCreateRequest request = new CommentCreateRequest(null, authorId, "댓글 내용 테스트");

      mockMvc.perform(post("/api/feeds/{feedId}/comments", feedId)
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - authorId 없으면 400 반환")
    void createComment_MissingAuthorId_Returns400() throws Exception {
      CommentCreateRequest request = new CommentCreateRequest(feedId, null, "댓글 내용 테스트");

      mockMvc.perform(post("/api/feeds/{feedId}/comments", feedId)
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - content 비어있으면 400 반환")
    void createComment_BlankContent_Returns400() throws Exception {
      CommentCreateRequest request = new CommentCreateRequest(feedId, authorId, "");

      mockMvc.perform(post("/api/feeds/{feedId}/comments", feedId)
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - content 2000자 초과하면 400 반환")
    void createComment_ContentExceedsMaxLength_Returns400() throws Exception {
      CommentCreateRequest request = new CommentCreateRequest(feedId, authorId, "a".repeat(2001));

      mockMvc.perform(post("/api/feeds/{feedId}/comments", feedId)
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  @Test
  @DisplayName("실패 - 락 획득 실패 시 409 반환")
  void like_LockAcquisitionFailed_Returns409() throws Exception {
    UUID userId = UUID.randomUUID();
    willThrow(new PessimisticLockingFailureException("lock timeout"))
        .given(feedService).increaseLikeCount(eq(feedId), eq(userId));

    mockMvc.perform(post("/api/feeds/{feedId}/like", feedId)
            .with(csrf())
            .param("userId", userId.toString()))
        .andExpect(status().isConflict());
  }

  @Nested
  @DisplayName("피드 목록 조회 관련 테스트")
  class CommentFindAllTests {
    @Test
    @DisplayName("성공 - 200 반환")
    void commentFindAll_Success() throws Exception {
      CommentPageRequest commentPageRequest =
          new CommentPageRequest(null, null, 2);

      CommentDtoCursorResponse response = new CommentDtoCursorResponse(
          List.of(commentDto1, commentDto2),
          null,
          null,
          false,
          2L,
          "createdAt",
          "DESCENDING"
      );

      given(feedService.findAll(eq(feedId), eq(commentPageRequest))).willReturn(response);

      mockMvc.perform(get("/api/feeds/{feedId}/comments", feedId)
              .param("limit", "2"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data").isArray())
          .andExpect(jsonPath("$.data.length()").value(2))
          .andExpect(jsonPath("$.data[0].content").value("댓글1"))
          .andExpect(jsonPath("$.data[1].content").value("댓글2"))
          .andExpect(jsonPath("$.hasNext").value(false))
          .andExpect(jsonPath("$.totalCount").value(2));
    }

    @Test
    @DisplayName("실패 - limit 없으면 400 반환")
    void commentFindAll_MissingLimit_Returns400() throws Exception {
      mockMvc.perform(get("/api/feeds/{feedId}/comments", feedId))
          .andExpect(status().isBadRequest());
    }
  }
}
