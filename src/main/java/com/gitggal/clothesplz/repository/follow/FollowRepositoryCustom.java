package com.gitggal.clothesplz.repository.follow;

import com.gitggal.clothesplz.entity.follow.Follow;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 팔로우 커스텀 Repository (QueryDSL)
 */
public interface FollowRepositoryCustom {

  /**
   * 팔로잉 목록 커서 기반 페이지네이션
   * @param followerId - 내 ID (내가 팔로우하는 목록)
   * @param nameLike   - 이름 검색 필터
   * @param cursor     - 이전 페이지 마지막 createdAt
   * @param idAfter    - 이전 페이지 마지막 follow ID
   * @param limit      - 가져올 개수
   */
  List<Follow> findFollowings(
      UUID followerId,
      String nameLike,
      Instant cursor,
      UUID idAfter,
      int limit
  );

  /**
   * 팔로워 목록 커서 기반 페이지네이션
   * @param followeeId  - 내 ID (나를 팔로우하는 목록)
   */
  List<Follow> findFollowers(
      UUID followeeId,
      String nameLike,
      Instant cursor,
      UUID idAfter,
      int limit
  );
}
