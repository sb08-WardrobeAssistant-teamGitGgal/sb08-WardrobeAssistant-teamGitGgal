package com.gitggal.clothesplz.service.feed;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.gitggal.clothesplz.dto.feed.CommentCreateRequest;
import com.gitggal.clothesplz.dto.feed.CommentDto;
import com.gitggal.clothesplz.dto.feed.CommentDtoCursorResponse;
import com.gitggal.clothesplz.dto.feed.CommentPageRequest;
import com.gitggal.clothesplz.dto.feed.FeedCreateRequest;
import com.gitggal.clothesplz.dto.feed.FeedDto;
import com.gitggal.clothesplz.dto.feed.FeedDtoCursorResponse;
import com.gitggal.clothesplz.dto.feed.FeedPageRequest;
import com.gitggal.clothesplz.dto.feed.FeedUpdateRequest;
import com.gitggal.clothesplz.dto.user.AuthorDto;
import java.time.Instant;
import com.gitggal.clothesplz.entity.feed.Feed;
import com.gitggal.clothesplz.entity.feed.FeedComment;
import com.gitggal.clothesplz.entity.feed.FeedLike;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.mapper.feed.CommentMapper;
import com.gitggal.clothesplz.mapper.feed.FeedMapper;
import com.gitggal.clothesplz.service.ServiceTestSupport;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ExtendWith(MockitoExtension.class)
@DisplayName("피드 서비스 테스트")
public class FeedServiceTest extends ServiceTestSupport {

  @Autowired
  private FeedService feedService;

  @MockitoBean
  private FeedMapper feedMapper;

  @MockitoBean
  private CommentMapper commentMapper;

  private UUID weatherId;
  private UUID authorId;
  private UUID userId;
  private UUID feedId;
  private Weather mockWeather;
  private User mockAuthor;
  private User mockUser;
  private Feed mockFeed;
  private FeedLike mockFeedLike;
  private FeedCreateRequest feedCreateRequest;
  private FeedUpdateRequest feedUpdateRequest;
  private CommentCreateRequest commentCreateRequest;
  private CommentPageRequest pageRequest;
  private CommentDto commentDto1;
  private CommentDto commentDto2;
  private FeedPageRequest feedPageRequest;
  private FeedDto feedDto1;
  private FeedDto feedDto2;
  private FeedDto feedDto3;

  @BeforeEach
  void setUp() {
    weatherId = UUID.randomUUID();
    authorId = UUID.randomUUID();
    userId = UUID.randomUUID();
    feedId = UUID.randomUUID();

    mockWeather = mock(Weather.class);
    mockAuthor = mock(User.class);
    mockUser = mock(User.class);
    mockFeed = mock(Feed.class);
    mockFeedLike = mock(FeedLike.class);

    feedCreateRequest = new FeedCreateRequest(
        authorId,
        weatherId,
        List.of(UUID.randomUUID()),
        "피드 생성"
    );

    feedUpdateRequest = new FeedUpdateRequest(
        "피드 수정"
    );

    commentCreateRequest = new CommentCreateRequest(
      feedId,
      authorId,
      "댓글 생성"
    );

    pageRequest = new CommentPageRequest(null, null, 2);
    AuthorDto authorDto = new AuthorDto(authorId, "작성자", "url");

    commentDto1 = new CommentDto(
        UUID.randomUUID(), Instant.now(), feedId, authorDto, "댓글1"
    );

    commentDto2 = new CommentDto(
        UUID.randomUUID(), Instant.now(), feedId, authorDto, "댓글2"
    );

    feedPageRequest = new FeedPageRequest(null, null, 2, "createdAt", "DESCENDING", null, null, null, null);

    feedDto1 = new FeedDto(
        UUID.randomUUID(), Instant.now(), Instant.now(),
        authorDto, null, List.of(),
        "피드1", 10L, 2, false
    );

    feedDto2 = new FeedDto(
        UUID.randomUUID(), Instant.now(), Instant.now(),
        authorDto, null, List.of(),
        "피드2", 5L, 1, false
    );

    feedDto3 = new FeedDto(
        UUID.randomUUID(), Instant.now(), Instant.now(),
        authorDto, null, List.of(),
        "피드3", 1L, 0, false
    );
  }

  @Nested
  @DisplayName("피드 생성 관련 테스트")
  class CreateFeedTests {

    @Test
    @DisplayName("피드 생성 성공인 경우")
    void createFeed_Success() {
      // given
      given(weatherRepository.findById(eq(weatherId))).willReturn(Optional.of(mockWeather));
      given(userRepository.findById(authorId)).willReturn(Optional.of(mockAuthor));
      given(feedRepository.save(any(Feed.class))).willAnswer(inv -> inv.getArgument(0));

      FeedDto expectedDto = mock(FeedDto.class);
      given(feedMapper.toDto(any(Feed.class))).willReturn(expectedDto);

      // when
      FeedDto result = feedService.createFeed(feedCreateRequest);

      // then
      assertThat(result).isEqualTo(expectedDto);
      then(feedRepository).should().save(any(Feed.class));
      then(feedMapper).should().toDto(any(Feed.class));
    }

    @Test
    @DisplayName("날씨 정보를 찾을 수 없는 경우 예외 발생")
    void createFeed_WeatherNotFound_ThrowsException() {
      // given
      given(weatherRepository.findById(eq(weatherId))).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> feedService.createFeed(feedCreateRequest))
          .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("피드 작성자 정보를 찾을 수 없는 경우 예외 발생")
    void createFeed_AuthorNotFound_ThrowsException() {
      // given
      given(weatherRepository.findById(eq(weatherId))).willReturn(Optional.of(mockWeather));
      given(userRepository.findById(eq(authorId))).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> feedService.createFeed(feedCreateRequest))
          .isInstanceOf(BusinessException.class);
    }
  }

  @Nested
  @DisplayName("피드 수정 관련 테스트")
  class UpdateFeedTests {

    @Test
    @DisplayName("피드 수정 성공인 경우")
    void updateFeed_Success() {
      // given
      given(feedRepository.findWithDetailsById(eq(feedId))).willReturn(Optional.of(mockFeed));

      FeedDto expectedDto = mock(FeedDto.class);
      given(feedMapper.toDto(any(Feed.class))).willReturn(expectedDto);

      // when
      FeedDto result = feedService.updateFeed(feedId, feedUpdateRequest);

      // then
      assertThat(result).isEqualTo(expectedDto);
    }

    @Test
    @DisplayName("피드 정보를 찾을 수 없는 경우 예외 발생")
    void updateFeed_FeedNotFound_ThrowsException() {
      // given
      given(feedRepository.findWithDetailsById(eq(feedId))).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> feedService.updateFeed(feedId, feedUpdateRequest))
          .isInstanceOf(BusinessException.class);
    }
  }

  @Nested
  @DisplayName("피드 삭제 관련 테스트")
  class DeleteFeedTests {

    @Test
    @DisplayName("피드 삭제 성공인 경우")
    void deleteFeed_Success() {
      // given
      given(feedRepository.findWithDetailsById(eq(feedId))).willReturn(Optional.of(mockFeed));

      // when
      feedService.deleteFeed(feedId);

      // then
      then(feedRepository).should().delete(any(Feed.class));
    }

    @Test
    @DisplayName("피드 정보를 찾을 수 없는 경우 예외 발생")
    void deleteFeed_FeedNotFound_ThrowsException() {
      // given
      given(feedRepository.findWithDetailsById(eq(feedId))).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> feedService.deleteFeed(feedId))
          .isInstanceOf(BusinessException.class);
    }
  }

  @Nested
  @DisplayName("피드 좋아요 관련 테스트")
  class IncreaseLikeTests {

    @Test
    @DisplayName("피드 좋아요 성공인 경우")
    void increaseLikeCount_Success() {
      // given
      given(feedRepository.findWithLockById(eq(feedId))).willReturn(Optional.of(mockFeed));
      given(userRepository.findById(eq(userId))).willReturn(Optional.of(mockUser));
      given(feedLikeRepository.existsByFeedIdAndUserId(eq(feedId), eq(userId))).willReturn(false);

      // when
      feedService.increaseLikeCount(feedId, userId);

      // then
      then(feedLikeRepository).should().save(any(FeedLike.class));
      then(mockFeed).should().increaseLikeCount();
    }

    @Test
    @DisplayName("피드 정보를 찾을 수 없는 경우 예외 발생")
    void increaseLikeCount_FeedNotFound_ThrowsException() {
      // given
      given(feedRepository.findWithLockById(eq(feedId))).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> feedService.increaseLikeCount(feedId, userId))
          .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("사용자 정보를 찾을 수 없는 경우 예외 발생")
    void increaseLikeCount_UserNotFound_ThrowsException() {
      // given
      given(feedRepository.findWithLockById(eq(feedId))).willReturn(Optional.of(mockFeed));
      given(userRepository.findById(eq(userId))).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> feedService.increaseLikeCount(feedId, userId))
          .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("이미 좋아요 한 경우 예외 발생")
    void increaseLikeCount_FeedLikeNotFound_ThrowsException() {
      // given
      given(feedRepository.findWithLockById(eq(feedId))).willReturn(Optional.of(mockFeed));
      given(userRepository.findById(eq(userId))).willReturn(Optional.of(mockUser));
      given(feedLikeRepository.existsByFeedIdAndUserId(eq(feedId), eq(userId))).willReturn(true);

      // when & then
      assertThatThrownBy(() -> feedService.increaseLikeCount(feedId, userId))
          .isInstanceOf(BusinessException.class);
    }
  }

  @Nested
  @DisplayName("피드 좋아요 취소 관련 테스트")
  class DecreaseLikeTests {

    @Test
    @DisplayName("피드 좋아요 취소 성공인 경우")
    void decreaseLikeCount_Success() {
      // given
      given(feedRepository.findWithLockById(eq(feedId))).willReturn(Optional.of(mockFeed));
      given(userRepository.findById(eq(userId))).willReturn(Optional.of(mockUser));
      given(feedLikeRepository.findByFeedIdAndUserId(eq(feedId), eq(userId))).willReturn(Optional.of(mockFeedLike));

      // when
      feedService.decreaseLikeCount(feedId, userId);

      // then
      then(feedLikeRepository).should().delete(any(FeedLike.class));
      then(mockFeed).should().decreaseLikeCount();
    }

    @Test
    @DisplayName("피드 정보를 찾을 수 없는 경우 예외 발생")
    void decreaseLikeCount_FeedNotFound_ThrowsException() {
      // given
      given(feedRepository.findWithLockById(eq(feedId))).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> feedService.decreaseLikeCount(feedId, userId))
          .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("사용자 정보를 찾을 수 없는 경우 예외 발생")
    void decreaseLikeCount_UserNotFound_ThrowsException() {
      // given
      given(feedRepository.findWithLockById(eq(feedId))).willReturn(Optional.of(mockFeed));
      given(userRepository.findById(eq(userId))).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> feedService.decreaseLikeCount(feedId, userId))
          .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("좋아요 정보를 찾을 수 없는 경우 예외 발생")
    void decreaseLikeCount_FeedLikeNotFound_ThrowsException() {
      // given
      given(feedRepository.findWithLockById(eq(feedId))).willReturn(Optional.of(mockFeed));
      given(userRepository.findById(eq(userId))).willReturn(Optional.of(mockUser));
      given(feedLikeRepository.findByFeedIdAndUserId(eq(feedId), eq(userId))).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> feedService.decreaseLikeCount(feedId, userId))
          .isInstanceOf(BusinessException.class);
    }
  }

  @Nested
  @DisplayName("피드 댓글 생성 관련 테스트")
  class CreateCommentTests {

    @Test
    @DisplayName("피드 댓글 생성 성공인 경우")
    void createComment_Success() {
      // given
      given(feedRepository.findWithLockById(eq(feedId))).willReturn(Optional.of(mockFeed));
      given(userRepository.findById(eq(authorId))).willReturn(Optional.of(mockAuthor));
      given(feedCommentRepository.save(any(FeedComment.class))).willAnswer(inv -> inv.getArgument(0));

      CommentDto expectedDto = mock(CommentDto.class);
      given(commentMapper.toDto(any(FeedComment.class))).willReturn(expectedDto);

      // when
      CommentDto result = feedService.createComment(feedId, commentCreateRequest);

      // then
      assertThat(result).isEqualTo(expectedDto);
      then(feedCommentRepository).should().save(any(FeedComment.class));
      then(mockFeed).should().increaseCommentCount();
    }

    @Test
    @DisplayName("피드 정보를 찾을 수 없는 경우 예외 발생")
    void createComment_FeedNotFound_ThrowsException() {
      // given
      given(feedRepository.findWithLockById(eq(feedId))).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> feedService.createComment(feedId, commentCreateRequest))
          .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("댓글 작성자 정보를 찾을 수 없는 경우 예외 발생")
    void createComment_authorNotFound_ThrowsException() {
      // given
      given(feedRepository.findWithLockById(eq(feedId))).willReturn(Optional.of(mockFeed));
      given(userRepository.findById(eq(authorId))).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> feedService.createComment(feedId, commentCreateRequest))
          .isInstanceOf(BusinessException.class);
    }
  }

  @Nested
  @DisplayName("댓글 목록 조회 관련 테스트")
  class GetCommentTests {

    @Test
    @DisplayName("댓글 목록 조회 성공 - 다음 페이지 없는 경우")
    void getComments_Success_NoNextPage() {
      // given
      given(feedRepository.findById(eq(feedId))).willReturn(Optional.of(mockFeed));
      given(feedCommentRepository.findAllByCursor(eq(feedId), eq(pageRequest)))
          .willReturn(List.of(commentDto1));
      given(mockFeed.getCommentCount()).willReturn(1L);

      // when
      CommentDtoCursorResponse result = feedService.getComments(feedId, pageRequest);

      // then
      assertThat(result.hasNext()).isFalse();
      assertThat(result.nextCursor()).isNull();
      assertThat(result.nextIdAfter()).isNull();
      assertThat(result.data().size()).isEqualTo(1);
      assertThat(result.totalCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("댓글 목록 조회 성공 - 다음 페이지 있는 경우")
    void getComments_Success_HasNextPage() {
      // given
      CommentDto commentDto3 = new CommentDto(
          UUID.randomUUID(), Instant.now(), feedId,
          new AuthorDto(authorId, "작성자3", "url3"), "댓글3"
      );
      given(feedRepository.findById(eq(feedId))).willReturn(Optional.of(mockFeed));
      given(feedCommentRepository.findAllByCursor(eq(feedId), eq(pageRequest)))
          .willReturn(List.of(commentDto1, commentDto2, commentDto3));
      given(mockFeed.getCommentCount()).willReturn(3L);

      // when
      CommentDtoCursorResponse result = feedService.getComments(feedId, pageRequest);

      // then
      assertThat(result.hasNext()).isTrue();
      assertThat(result.nextCursor()).isEqualTo(commentDto2.createdAt().toString());
      assertThat(result.nextIdAfter()).isEqualTo(commentDto2.id());
      assertThat(result.data().size()).isEqualTo(2);
      assertThat(result.totalCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("피드에 댓글이 없어 빈 목록 반환하는 경우")
    void getComments_EmptyComments() {
      // given
      given(feedRepository.findById(eq(feedId))).willReturn(Optional.of(mockFeed));
      given(feedCommentRepository.findAllByCursor(eq(feedId), eq(pageRequest)))
          .willReturn(List.of());
      given(mockFeed.getCommentCount()).willReturn(0L);

      // when
      CommentDtoCursorResponse result = feedService.getComments(feedId, pageRequest);

      // then
      assertThat(result.hasNext()).isFalse();
      assertThat(result.data().size()).isEqualTo(0);
      assertThat(result.totalCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("피드 정보를 찾을 수 없는 경우 예외 발생")
    void getComments_FeedNotFound_ThrowsException() {
      // given
      given(feedRepository.findById(eq(feedId))).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> feedService.getComments(feedId, pageRequest))
          .isInstanceOf(BusinessException.class);
    }
  }

  @Nested
  @DisplayName("피드 목록 조회 관련 테스트")
  class GetFeedTests {

    @Test
    @DisplayName("피드 목록 조회 성공 - 다음 페이지 없는 경우")
    void getFeeds_Success_NoNextPage() {
      // given
      given(feedRepository.findAllByCursor(eq(feedPageRequest))).willReturn(List.of(feedDto1));
      given(feedLikeRepository.findFeedIdsByUserId(eq(userId), any())).willReturn(Set.of());
      given(feedRepository.countByCondition(eq(feedPageRequest))).willReturn(1L);

      // when
      FeedDtoCursorResponse result = feedService.getFeeds(userId, feedPageRequest);

      // then
      assertThat(result.hasNext()).isFalse();
      assertThat(result.nextCursor()).isNull();
      assertThat(result.nextIdAfter()).isNull();
      assertThat(result.data().size()).isEqualTo(1);
      assertThat(result.totalCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("피드 목록 조회 성공 - 다음 페이지 있는 경우 (sortBy=createdAt)")
    void getFeeds_Success_HasNextPage_SortByCreatedAt() {
      // given
      given(feedRepository.findAllByCursor(eq(feedPageRequest)))
          .willReturn(List.of(feedDto1, feedDto2, feedDto3));
      given(feedLikeRepository.findFeedIdsByUserId(eq(userId), any())).willReturn(Set.of());
      given(feedRepository.countByCondition(eq(feedPageRequest))).willReturn(3L);

      // when
      FeedDtoCursorResponse result = feedService.getFeeds(userId, feedPageRequest);

      // then
      assertThat(result.hasNext()).isTrue();
      assertThat(result.data().size()).isEqualTo(2);
      assertThat(result.nextCursor()).isEqualTo(feedDto2.createdAt().toString());
      assertThat(result.nextIdAfter()).isEqualTo(feedDto2.id());
    }

    @Test
    @DisplayName("피드 목록 조회 성공 - 다음 페이지 있는 경우 (sortBy=likeCount)")
    void getFeeds_Success_HasNextPage_SortByLikeCount() {
      // given
      FeedPageRequest likeCountRequest = new FeedPageRequest(
          null, null, 2, "likeCount", "DESCENDING", null, null, null, null
      );
      given(feedRepository.findAllByCursor(eq(likeCountRequest)))
          .willReturn(List.of(feedDto1, feedDto2, feedDto3));
      given(feedLikeRepository.findFeedIdsByUserId(eq(userId), any())).willReturn(Set.of());
      given(feedRepository.countByCondition(eq(likeCountRequest))).willReturn(3L);

      // when
      FeedDtoCursorResponse result = feedService.getFeeds(userId, likeCountRequest);

      // then
      assertThat(result.hasNext()).isTrue();
      assertThat(result.nextCursor()).isEqualTo(String.valueOf(feedDto2.likeCount()));
      assertThat(result.nextIdAfter()).isEqualTo(feedDto2.id());
    }

    @Test
    @DisplayName("피드 목록이 비어있는 경우")
    void getFeeds_EmptyList() {
      // given
      given(feedRepository.findAllByCursor(eq(feedPageRequest))).willReturn(List.of());
      given(feedLikeRepository.findFeedIdsByUserId(eq(userId), any())).willReturn(Set.of());
      given(feedRepository.countByCondition(eq(feedPageRequest))).willReturn(0L);

      // when
      FeedDtoCursorResponse result = feedService.getFeeds(userId, feedPageRequest);

      // then
      assertThat(result.data().size()).isEqualTo(0);
      assertThat(result.hasNext()).isFalse();
      assertThat(result.nextCursor()).isNull();
      assertThat(result.nextIdAfter()).isNull();
    }

    @Test
    @DisplayName("사용자가 좋아요한 피드에만 likedByMe가 true로 매핑되는 경우")
    void getFeeds_LikedByMe_MappedCorrectly() {
      // given
      given(feedRepository.findAllByCursor(eq(feedPageRequest)))
          .willReturn(List.of(feedDto1, feedDto2));
      given(feedLikeRepository.findFeedIdsByUserId(eq(userId), any()))
          .willReturn(Set.of(feedDto1.id()));
      given(feedRepository.countByCondition(eq(feedPageRequest))).willReturn(2L);

      // when
      FeedDtoCursorResponse result = feedService.getFeeds(userId, feedPageRequest);

      // then
      assertThat(result.data().get(0).likedByMe()).isTrue();
      assertThat(result.data().get(1).likedByMe()).isFalse();
    }
  }
}
