package com.gitggal.clothesplz.controller.follow;

import com.gitggal.clothesplz.dto.follow.FollowListResponse;
import com.gitggal.clothesplz.dto.follow.FollowSummaryDto;
import com.gitggal.clothesplz.service.follow.FollowService;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
      @RequestParam UUID requesterId) {   // 현재 로그인 사용자

    log.info("[Controller] 팔로우 요약 조회 요청 시작: userId={}", userId);

    FollowSummaryDto response = followService.getFollowSummary(userId, requesterId);

    log.info("[Controller] 팔로우 요약 조회 요청 완료: userId={}", userId);

    return ResponseEntity.ok(response);
  }
}
