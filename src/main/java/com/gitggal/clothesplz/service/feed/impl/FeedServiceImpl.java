package com.gitggal.clothesplz.service.feed.impl;

import com.gitggal.clothesplz.dto.clothes.OotdDto;
import com.gitggal.clothesplz.dto.feed.CommentCreateRequest;
import com.gitggal.clothesplz.dto.feed.CommentDto;
import com.gitggal.clothesplz.dto.feed.FeedCreateRequest;
import com.gitggal.clothesplz.dto.feed.FeedDto;
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
import com.gitggal.clothesplz.repository.user.UserRepository;
import com.gitggal.clothesplz.repository.weather.WeatherRepository;
import com.gitggal.clothesplz.service.feed.FeedService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class FeedServiceImpl implements FeedService {

  private final UserRepository userRepository;
  private final WeatherRepository weatherRepository;
  private final FeedRepository feedRepository;
  private final FeedLikeRepository feedLikeRepository;
  private final FeedCommentRepository feedCommentRepository;
  private final FeedMapper feedMapper;
  private final CommentMapper commentMapper;

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

    log.info("[Service] 피드 생성 요청 완료 - feedId: {}", savedFeed.getId());

    return feedMapper.toDto(savedFeed);
  }

  // TODO: 관리자나 피드 작성자만 피드 수정하도록 권한 위임 예정
  @Override
  @Transactional
  public FeedDto updateFeed(UUID feedId, FeedUpdateRequest feedUpdateRequest) {
    log.info("[Service] 피드 수정 요청 시작 - feedId: {}", feedId);
    String newContent = feedUpdateRequest.content();

    Feed feed = feedRepository.findWithDetailsById(feedId)
        .orElseThrow(() -> new BusinessException(FeedErrorCode.FEED_NOT_FOUND));

    feed.update(newContent);

    log.info("[Service] 피드 수정 요청 완료 - feedId: {}", feedId);

    return feedMapper.toDto(feed);
  }

  // TODO: 관리자나 피드 작성자만 피드 삭제하도록 권한 위임 예정
  @Override
  @Transactional
  public void deleteFeed(UUID feedId) {
    log.info("[Service] 피드 삭제 요청 시작 - feedId: {}", feedId);

    Feed feed = feedRepository.findWithDetailsById(feedId)
        .orElseThrow(() -> new BusinessException(FeedErrorCode.FEED_NOT_FOUND));

    feedRepository.delete(feed);
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

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    FeedLike feedLike = feedLikeRepository.findByFeedIdAndUserId(feedId, userId)
        .orElseThrow(() -> new BusinessException(FeedErrorCode.FEED_LIKE_NOT_FOUND));

    feedLikeRepository.delete(feedLike);
    feed.decreaseLikeCount();

    log.info("[Service] 피드 좋아요 취소 요청 완료 - feedLikeId: {}", feedLike.getId());
  }

  @Override
  @Transactional
  public CommentDto createComment(CommentCreateRequest commentCreateRequest) {
    log.info("[Service] 피드 댓글 생성 요청 시작 - feedId: {}, authorId: {}",
        commentCreateRequest.feedId(), commentCreateRequest.authorId());

    UUID feedId = commentCreateRequest.feedId();
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
}
