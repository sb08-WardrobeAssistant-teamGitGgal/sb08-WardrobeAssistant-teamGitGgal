package com.gitggal.clothesplz.controller.follow;

import com.gitggal.clothesplz.dto.follow.FollowCreateRequest;
import com.gitggal.clothesplz.dto.follow.FollowDto;
import com.gitggal.clothesplz.dto.follow.FollowListResponse;
import com.gitggal.clothesplz.dto.follow.FollowSummaryDto;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import com.gitggal.clothesplz.service.follow.FollowService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 팔로우 관련 Controller
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

  private final FollowService followService;

  @PostMapping
  public ResponseEntity<FollowDto> createFollow(
      @RequestBody @Valid FollowCreateRequest request) {

    log.info("[Controller] 팔로우 생성 요청 시작: followerId={}, followeeId={}", request.followerId(), request.followeeId());

    FollowDto response = followService.createFollow(request);

    log.info("[Controller] 팔로우 생성 요청 완료: followerId={}, followeeId={}", request.followerId(), request.followeeId());

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }


  @DeleteMapping("/{followId}")
  public ResponseEntity<Void> cancelFollow(
      @PathVariable UUID followId) {

    log.info("[Controller] 팔로우 취소 요청 시작: followId={}", followId);

    followService.cancelFollow(followId);

    log.info("[Controller] 팔로우 취소 요청 완료: followId={}", followId);

    return ResponseEntity.noContent().build();
  }


  @GetMapping("/followings")
  public ResponseEntity<FollowListResponse> getFollowings(
      @RequestParam UUID followerId,
      @RequestParam(required = false) String nameLike,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam @Positive int limit) {

    log.info("[Controller] 팔로잉(우) 목록 조회 요청 시작: followerId={}", followerId);

    FollowListResponse response =
        followService.getFollowings(followerId, nameLike, cursor, idAfter, limit);

    log.info("[Controller] 팔로잉(우) 목록 조회 요청 완료: followerId={}", followerId);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/followers")
  public ResponseEntity<FollowListResponse> getFollowers(
      @RequestParam UUID followeeId,
      @RequestParam(required = false) String nameLike,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam @Positive int limit) {

    log.info("[Controller] 팔로워 목록 조회 요청 시작: followeeId={}", followeeId);

    FollowListResponse response =
        followService.getFollowers(followeeId, nameLike, cursor, idAfter, limit);

    log.info("[Controller] 팔로워 목록 조회 요청 완료: followeeId={}", followeeId);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/summary")
  public ResponseEntity<FollowSummaryDto> getFollowSummary(
      @RequestParam UUID userId,
      @AuthenticationPrincipal ClothesUserDetails userDetails) {   // 현재 로그인 사용자

    UUID requesterId = userDetails.getUserDto().id();

    log.info("[Controller] 팔로우 요약 조회 요청 시작: userId={}", userId);

    FollowSummaryDto response = followService.getFollowSummary(userId, requesterId);

    log.info("[Controller] 팔로우 요약 조회 요청 완료: userId={}", userId);

    return ResponseEntity.ok(response);
  }
}
