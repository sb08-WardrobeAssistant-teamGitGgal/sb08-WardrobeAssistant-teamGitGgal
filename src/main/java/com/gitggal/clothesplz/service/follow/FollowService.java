package com.gitggal.clothesplz.service.follow;

import com.gitggal.clothesplz.dto.follow.FollowCreateRequest;
import com.gitggal.clothesplz.dto.follow.FollowDto;
import com.gitggal.clothesplz.dto.follow.FollowListResponse;
import com.gitggal.clothesplz.dto.follow.FollowSummaryDto;
import java.util.UUID;

/**
 * 팔로우 서비스 인터페이스
 */
public interface FollowService {

  /**
   * 팔로우 생성 - 팔로우 대상에게 알림 발송
   */
  FollowDto createFollow(FollowCreateRequest request);

  /**
   * 팔로우 취소
   */
  void cancelFollow(UUID followId);

  /**
   * 팔로잉(우) 목록 - 내가 팔로우 하는 목록
   */
  FollowListResponse getFollowings(
      UUID followerId,
      String nameLike,
      String cursor,
      UUID idAfter,
      int limit
  );

  /**
   * 팔로워 목록 - 나를 팔로우하는 목록
   */
  FollowListResponse getFollowers(
      UUID followeeId,
      String nameLike,
      String cursor,
      UUID idAfter,
      int limit
  );

  /**
   * 팔로우 요약 조회
   */
  FollowSummaryDto getFollowSummary(UUID userId, UUID requesterId);
}
