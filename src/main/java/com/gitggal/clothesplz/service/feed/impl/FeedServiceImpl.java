package com.gitggal.clothesplz.service.feed.impl;

import com.gitggal.clothesplz.document.feed.FeedDocument;
import com.gitggal.clothesplz.dto.clothes.OotdDto;
import com.gitggal.clothesplz.dto.feed.CommentCreateRequest;
import com.gitggal.clothesplz.dto.feed.CommentDto;
import com.gitggal.clothesplz.dto.feed.CommentDtoCursorResponse;
import com.gitggal.clothesplz.dto.feed.CommentPageRequest;
import com.gitggal.clothesplz.dto.feed.FeedCreateRequest;
import com.gitggal.clothesplz.dto.feed.FeedCursorCondition;
import com.gitggal.clothesplz.dto.feed.FeedDto;
import com.gitggal.clothesplz.dto.feed.FeedDtoCursorResponse;
import com.gitggal.clothesplz.dto.feed.FeedPageRequest;
import com.gitggal.clothesplz.dto.feed.FeedUpdateRequest;
import com.gitggal.clothesplz.entity.feed.Feed;
import com.gitggal.clothesplz.entity.feed.FeedComment;
import com.gitggal.clothesplz.entity.feed.FeedLike;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.FeedErrorCode;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.exception.code.WeatherErrorCode;
import com.gitggal.clothesplz.mapper.feed.CommentMapper;
import com.gitggal.clothesplz.mapper.feed.FeedMapper;
import com.gitggal.clothesplz.repository.feed.FeedCommentRepository;
import com.gitggal.clothesplz.repository.feed.FeedLikeRepository;
import com.gitggal.clothesplz.repository.feed.FeedRepository;
import com.gitggal.clothesplz.repository.feed.FeedSearchRepository;
import com.gitggal.clothesplz.repository.user.UserRepository;
import com.gitggal.clothesplz.repository.weather.WeatherRepository;
import com.gitggal.clothesplz.service.feed.FeedService;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class FeedServiceImpl implements FeedService {

  private static final String COMMENT_SORT_BY = "createdAt";
  private static final String COMMENT_SORT_DIRECTION = "DESCENDING";

  private final UserRepository userRepository;
  private final WeatherRepository weatherRepository;
  private final FeedRepository feedRepository;
  private final FeedLikeRepository feedLikeRepository;
  private final FeedCommentRepository feedCommentRepository;
  private final FeedMapper feedMapper;
  private final CommentMapper commentMapper;
  private final FeedSearchRepository feedSearchRepository;

  @Override
  @Transactional
  public FeedDto createFeed(FeedCreateRequest feedCreateRequest) {
    log.info("[Service] 피드 생성 요청 시작 - authorId: {}, weatherId: {}",
        feedCreateRequest.authorId(), feedCreateRequest.weatherId());
    UUID weatherId = feedCreateRequest.weatherId();
    UUID authorId = feedCreateRequest.authorId();
    List<UUID> clothesId = feedCreateRequest.clothesIds();
    String content = feedCreateRequest.content();

    Weather weather = weatherRepository.findById(weatherId)
        .orElseThrow(() -> new BusinessException(WeatherErrorCode.WEATHER_NOT_FOUND));

    User author = userRepository.findById(authorId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    // TODO: clothesService 구현 후 교체
    // 현재 clothesIds는 수신되지만 ootds에 반영되지 않음 (의상 추천 기능 미구현 상태)
    List<OotdDto> ootds = List.of();

    Feed feed = new Feed(weather, author, ootds, content);

    Feed savedFeed = feedRepository.save(feed);

    feedSearchRepository.save(FeedDocument.builder()
        .id(savedFeed.getId().toString())
        .content(savedFeed.getContent())
        .authorId(savedFeed.getAuthor().getId().toString())
        .skyStatus(savedFeed.getWeather().getSkyStatus().name())
        .precipitationType(savedFeed.getWeather().getPrecipitationType().name())
        .likeCount(savedFeed.getLikeCount())
        .createdAt(savedFeed.getCreatedAt())
        .build());

    log.info("[Service] 피드 생성 요청 완료 - feedId: {}", savedFeed.getId());

    return feedMapper.toDto(savedFeed);
  }

  @PreAuthorize("isAuthenticated() and (hasRole('ADMIN') or @feedServiceImpl.isAuthor(#feedId, authentication.principal.userDto.id))")
  @Override
  @Transactional
  public FeedDto updateFeed(UUID feedId, FeedUpdateRequest feedUpdateRequest) {
    log.info("[Service] 피드 수정 요청 시작 - feedId: {}", feedId);
    String newContent = feedUpdateRequest.content();

    Feed feed = feedRepository.findWithDetailsById(feedId)
        .orElseThrow(() -> new BusinessException(FeedErrorCode.FEED_NOT_FOUND));

    feed.update(newContent);

    feedSearchRepository.findById(feedId.toString()).ifPresent(doc ->
        feedSearchRepository.save(FeedDocument.builder()
            .id(doc.getId())
            .content(feed.getContent())
            .authorId(doc.getAuthorId())
            .skyStatus(doc.getSkyStatus())
            .precipitationType(doc.getPrecipitationType())
            .likeCount(doc.getLikeCount())
            .createdAt(doc.getCreatedAt())
            .build())
    );

    log.info("[Service] 피드 수정 요청 완료 - feedId: {}", feedId);

    return feedMapper.toDto(feed);
  }

  @PreAuthorize("isAuthenticated() and (hasRole('ADMIN') or @feedServiceImpl.isAuthor(#feedId, authentication.principal.userDto.id))")
  @Override
  @Transactional
  public void deleteFeed(UUID feedId) {
    log.info("[Service] 피드 삭제 요청 시작 - feedId: {}", feedId);

    Feed feed = feedRepository.findWithDetailsById(feedId)
        .orElseThrow(() -> new BusinessException(FeedErrorCode.FEED_NOT_FOUND));

    feedRepository.delete(feed);
    feedSearchRepository.deleteById(feedId.toString());

    log.info("[Service] 피드 삭제 요청 완료 - feedId: {}", feedId);
  }

  @Override
  @Transactional
  public void increaseLikeCount(UUID feedId, UUID userId) {
    log.info("[Service] 피드 좋아요 요청 시작 - feedId: {}, userId: {}", feedId, userId);

    Feed feed = feedRepository.findWithLockById(feedId)
        .orElseThrow(() -> new BusinessException(FeedErrorCode.FEED_NOT_FOUND));

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    // 사용자가 피드의 좋아요를 이미 눌렀는지 검증
    if (feedLikeRepository.existsByFeedIdAndUserId(feedId, userId)) {
      throw new BusinessException(FeedErrorCode.FEED_LIKE_ALREADY_EXISTS);
    }

    FeedLike feedLike = new FeedLike(feed, user);

    feedLikeRepository.save(feedLike);
    feed.increaseLikeCount();

    log.info("[Service] 피드 좋아요 요청 완료 - feedLikeId: {}", feedLike.getId());
  }

  @Override
  @Transactional
  public void decreaseLikeCount(UUID feedId, UUID userId) {
    log.info("[Service] 피드 좋아요 취소 요청 시작 - feedId: {}, userId: {}", feedId, userId);

    Feed feed = feedRepository.findWithLockById(feedId)
        .orElseThrow(() -> new BusinessException(FeedErrorCode.FEED_NOT_FOUND));

    FeedLike feedLike = feedLikeRepository.findByFeedIdAndUserId(feedId, userId)
        .orElseThrow(() -> new BusinessException(FeedErrorCode.FEED_LIKE_NOT_FOUND));

    feedLikeRepository.delete(feedLike);
    feed.decreaseLikeCount();

    log.info("[Service] 피드 좋아요 취소 요청 완료 - feedLikeId: {}", feedLike.getId());
  }

  @Override
  @Transactional
  public CommentDto createComment(UUID feedId, CommentCreateRequest commentCreateRequest) {
    log.info("[Service] 피드 댓글 생성 요청 시작 - feedId: {}, authorId: {}",
        feedId, commentCreateRequest.authorId());

    UUID authorId = commentCreateRequest.authorId();
    String content = commentCreateRequest.content();

    Feed feed = feedRepository.findWithLockById(feedId)
        .orElseThrow(() -> new BusinessException(FeedErrorCode.FEED_NOT_FOUND));

    User author = userRepository.findById(authorId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    FeedComment comment = new FeedComment(feed, author, content);
    FeedComment savedComment = feedCommentRepository.save(comment);
    feed.increaseCommentCount();

    log.info("[Service] 피드 댓글 생성 요청 완료 - commentId: {}", savedComment.getId());
    return commentMapper.toDto(savedComment);
  }

  @Override
  public CommentDtoCursorResponse getComments(UUID feedId, CommentPageRequest commentPageRequest) {
    log.info("[Service] 피드 댓글 목록 조회 요청 시작 - feedId: {}", feedId);

    Instant cursorInstant;
    try {
      cursorInstant =
          (commentPageRequest.cursor() != null && !commentPageRequest.cursor().isBlank())
              ? Instant.parse(commentPageRequest.cursor())
              : null;
    } catch (DateTimeParseException e) {
      throw new BusinessException(FeedErrorCode.INVALID_CURSOR_FORMAT);
    }

    Feed feed = feedRepository.findById(feedId)
        .orElseThrow(() -> new BusinessException(FeedErrorCode.FEED_NOT_FOUND));

    List<CommentDto> comments = feedCommentRepository.findAllByCursor(feedId, commentPageRequest,
        cursorInstant);

    boolean hasNext = comments.size() > commentPageRequest.limit();
    List<CommentDto> data = hasNext ? comments.subList(0, commentPageRequest.limit()) : comments;

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (!data.isEmpty() && hasNext) {
      CommentDto lastComment = data.get(data.size() - 1);
      nextCursor = lastComment.createdAt().toString();
      nextIdAfter = lastComment.id();
    }

    long totalCount = feed.getCommentCount();

    log.info("[Service] 피드 댓글 목록 조회 요청 완료 - commentCount: {}", totalCount);

    return new CommentDtoCursorResponse(
        data,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        COMMENT_SORT_BY,
        COMMENT_SORT_DIRECTION
    );
  }

  @Override
  public FeedDtoCursorResponse getFeeds(UUID userId, FeedPageRequest feedPageRequest) {
    log.info("[Service] 피드 목록 조회 요청 시작 - limit: {}", feedPageRequest.limit());

    // 요청 cursor를 받아서 parse
    FeedCursorCondition feedCursorCondition = parseCursor(
        feedPageRequest.cursor(),
        feedPageRequest.idAfter(),
        feedPageRequest.sortBy()
    );

    List<UUID> esMatchedIDs = null;
    if (StringUtils.hasText(feedPageRequest.keywordLike())) {
      List<FeedDocument> documents = feedSearchRepository.searchByContent(
          feedPageRequest.keywordLike());

      // search 검사 결과 아무것도 없을 경우 빈 페이지 반환
      if (documents.isEmpty()) {
        return new FeedDtoCursorResponse(
          List.of(), null, null, false, 0,
          feedPageRequest.sortBy(), feedPageRequest.sortDirection()
        );
      }

      // 검사 결과와 일치하는 피드들의 id만 추출해서 담기
      esMatchedIDs = documents.stream()
          .map(document -> UUID.fromString(document.getId()))
          .toList();
    }

    // 그 후 필터링, 정렬, 커서 페이지네이션 처리 후 반환
    List<FeedDto> feeds = feedRepository.findAllByCursor(feedPageRequest, feedCursorCondition, esMatchedIDs);

    boolean hasNext = feeds.size() > feedPageRequest.limit();
    List<FeedDto> data = hasNext ? feeds.subList(0, feedPageRequest.limit()) : feeds;

    // 피드 목록의 id들만 추출
    List<UUID> feedIds = data.stream().map(FeedDto::id).toList();

    // 피드 목록 조회 요청 보낸 사용자가 좋아요를 누른 피드 목록
    Set<UUID> likedFeedIds = feedLikeRepository.findFeedIdsByUserId(userId, feedIds);

    // likedByMe 값을 채우기 위해 2차 매핑
    List<FeedDto> result = data.stream()
        // 피드 목록을 순회하며 사용자가 좋아요를 누른 피드인지 검증
        .map(feedDto -> feedDto.withLikedByMe(likedFeedIds.contains(feedDto.id())))
        .toList();

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (!data.isEmpty() && hasNext) {
      FeedDto lastFeedDto = data.get(data.size() - 1);
      nextIdAfter = lastFeedDto.id();

      if ("likeCount".equals(feedPageRequest.sortBy())) {
        nextCursor = String.valueOf(lastFeedDto.likeCount());
      } else {
        nextCursor = lastFeedDto.createdAt().toString();
      }
    }

    long totalCount = feedRepository.countByCondition(feedPageRequest, esMatchedIDs);

    log.info("[Service] 피드 목록 조회 요청 완료 - totalCount: {}", totalCount);

    return new FeedDtoCursorResponse(
        result,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        feedPageRequest.sortBy(),
        feedPageRequest.sortDirection()
    );
  }

  private FeedCursorCondition parseCursor(String cursor, UUID idAfter, String sortBy) {
    if (cursor == null || idAfter == null) {
      // 첫 페이지인 경우 커서 x
      return new FeedCursorCondition(null, null, null);
    }

    try {
      if ("likeCount".equals(sortBy)) {
        // 좋아요 수가 정렬 기준일 경우 cursor 자료형은 Long
        return new FeedCursorCondition(null, Long.parseLong(cursor), idAfter);
      } else {
        // 생성 시간이 정렬 기준일 경우 cursor 자료형은 Instant
        return new FeedCursorCondition(Instant.parse(cursor), null, idAfter);
      }
    } catch (Exception e) {
      throw new BusinessException(FeedErrorCode.INVALID_CURSOR_FORMAT);
    }
  }

  // 권한 검증용 메소드(작성자가 userId인 feed가 있나 없나)
  public boolean isAuthor(UUID feedId, UUID userId) {
    return feedRepository.existsByIdAndAuthorId(feedId, userId);
  }
}
