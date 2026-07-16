package com.gitggal.clothesplz.service.follow;

import java.util.Optional;
import java.util.UUID;

/**
 * 팔로워 수 / 팔로잉 수 Redis 캐시 관련 인터페이스
 * 캐시 미스일 때 DB 조회할지 여부는 인터페이스를 호출하는 FollowServiceImpl 쪽에서 결정
 */
public interface FollowCountCacheService {

  Optional<Long> getFollowerCount(UUID userId);

  Optional<Long> getFollowingCount(UUID userId);

  void saveFollowerCount(UUID userId, long count);

  void saveFollowingCount(UUID userId, long count);

  void increaseFollowerCount(UUID userId);

  void decreaseFollowerCount(UUID userId);

  void increaseFollowingCount(UUID userId);

  void decreaseFollowingCount(UUID userId);
}
