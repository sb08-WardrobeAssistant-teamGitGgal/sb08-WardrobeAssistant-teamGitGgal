package com.gitggal.clothesplz.controller.feed;

import com.gitggal.clothesplz.dto.feed.CommentCreateRequest;
import com.gitggal.clothesplz.dto.feed.CommentDto;
import com.gitggal.clothesplz.dto.feed.CommentDtoCursorResponse;
import com.gitggal.clothesplz.dto.feed.CommentPageRequest;
import com.gitggal.clothesplz.dto.feed.FeedCreateRequest;
import com.gitggal.clothesplz.dto.feed.FeedDto;
import com.gitggal.clothesplz.dto.feed.FeedDtoCursorResponse;
import com.gitggal.clothesplz.dto.feed.FeedPageRequest;
import com.gitggal.clothesplz.dto.feed.FeedUpdateRequest;
import com.gitggal.clothesplz.service.feed.FeedService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/feeds")
public class FeedController {

  private final FeedService feedService;

  @PostMapping
  public ResponseEntity<FeedDto> create(
      @Valid @RequestBody FeedCreateRequest feedCreateRequest
  ) {
    log.info("[Controller] 피드 생성 요청 시작");
    FeedDto feedDto = feedService.createFeed(feedCreateRequest);

    log.info("[Controller] 피드 생성 요청 완료");
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(feedDto);
  }

  @PatchMapping("/{feedId}")
  public ResponseEntity<FeedDto> update(
      @PathVariable UUID feedId,
      @Valid @RequestBody FeedUpdateRequest feedUpdateRequest
  ) {
    log.info("[Controller] 피드 수정 요청 시작");
    FeedDto feedDto = feedService.updateFeed(feedId, feedUpdateRequest);

    log.info("[Controller] 피드 수정 요청 완료");
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(feedDto);
  }

  @DeleteMapping("/{feedId}")
  public ResponseEntity<Void> delete(
      @PathVariable UUID feedId
  ) {
    log.info("[Controller] 피드 삭제 요청 시작");
    feedService.deleteFeed(feedId);

    log.info("[Controller] 피드 삭제 요청 완료");
    return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
  }

  // TODO: security 구현 시 @AuthenticationPrincipal로 교체
  @PostMapping("/{feedId}/like")
  public ResponseEntity<Void> like(
      @PathVariable UUID feedId,
      @RequestParam UUID userId
  ) {
    log.info("[Controller] 피드 좋아요 요청 시작");
    feedService.increaseLikeCount(feedId, userId);

    log.info("[Controller] 피드 좋아요 요청 완료");
    return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
  }

  // TODO: security 구현 시 @AuthenticationPrincipal로 교체
  @DeleteMapping("/{feedId}/like")
  public ResponseEntity<Void> cancelLike(
      @PathVariable UUID feedId,
      @RequestParam UUID userId
  ) {
    log.info("[Controller] 피드 좋아요 취소 요청 시작");
    feedService.decreaseLikeCount(feedId, userId);

    log.info("[Controller] 피드 좋아요 취소 요청 완료");
    return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
  }

  @PostMapping("/{feedId}/comments")
  public ResponseEntity<CommentDto> comment(
      @PathVariable UUID feedId,
      @Valid @RequestBody CommentCreateRequest commentCreateRequest
  ) {
    log.info("[Controller] 피드 댓글 등록 요청 시작");
    CommentDto commentDto = feedService.createComment(feedId, commentCreateRequest);


    log.info("[Controller] 피드 댓글 등록 요청 완료");
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(commentDto);
  }

  @GetMapping("/{feedId}/comments")
  public ResponseEntity<CommentDtoCursorResponse> getComments(
      @PathVariable UUID feedId,
      @Valid @ModelAttribute CommentPageRequest commentPageRequest
  ) {
    log.info("[Controller] 피드 댓글 목록 조회 요청 시작");
    CommentDtoCursorResponse commentDtoCursorResponse = feedService.getComments(feedId, commentPageRequest);


    log.info("[Controller] 피드 댓글 목록 조회 요청 완료");
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(commentDtoCursorResponse);
  }

  // TODO: security 구현 시 @AuthenticationPrincipal로 교체
  @GetMapping
  public ResponseEntity<FeedDtoCursorResponse> getFeeds(
      @Valid @ModelAttribute FeedPageRequest feedPageRequest,
      @RequestParam UUID userId
  ) {
    log.info("[Controller] 피드  목록 조회 요청 시작");
    FeedDtoCursorResponse feedDtoCursorResponse = feedService.getFeeds(userId, feedPageRequest);

    log.info("[Controller] 피드  목록 조회 요청 완료");
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(feedDtoCursorResponse);
  }
}
