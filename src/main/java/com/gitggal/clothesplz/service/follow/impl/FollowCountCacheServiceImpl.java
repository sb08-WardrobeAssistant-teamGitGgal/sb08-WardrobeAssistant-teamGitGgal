package com.gitggal.clothesplz.service.follow.impl;

import com.gitggal.clothesplz.service.follow.FollowCountCacheService;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowCountCacheServiceImpl implements FollowCountCacheService {

  private static final String FOLLOWER_KEY_PREFIX = "follow:followerCount:";

  private static final String FOLLOWING_KEY_PREFIX = "follow:followingCount:";

  // 캐시 유지 최대 시간
  private static final Duration COUNT_TTL = Duration.ofHours(1);

  private final StringRedisTemplate stringRedisTemplate;

  /**
   *  "나를 팔로우하는 사람 수"를 캐시에서 읽어옴
   *  캐시에 값이 없을 수도 있기에 반환 타입 Optional로 지정
   */
  @Override
  public Optional<Long> getFollowerCount(UUID userId) {
    return getCount(followerKey(userId));
  }

  /**
   *  "내가 팔로우하는 사람 수"를 캐시에서 읽어옴
   */
  @Override
  public Optional<Long> getFollowingCount(UUID userId) {
    return getCount(followingKey(userId));
  }

  /**
   * DB에서 방금 계산한 팔로워 수를 캐시에 새로 저장
   */
  @Override
  public void saveFollowerCount(UUID userId, long count) {
    saveCount(followerKey(userId), count);
  }

  /**
   * DB에서 방금 계산한 팔로잉 수를 캐시에 새로 저장
   */
  @Override
  public void saveFollowingCount(UUID userId, long count) {
    saveCount(followingKey(userId), count);
  }

  /**
   * 해당 유저를 누군가 새로 팔로우했을 때 -> (캐시에 저장된 팔로워 수 + 1)
   */
  @Override
  public void increaseFollowerCount(UUID userId) {
    incrementIfPresent(followerKey(userId));    // 팔로우 생성 시 + 1
  }

  /**
   * 해당 유저에 대한 팔로우를 취소했을 때 (캐시에 저장된 팔로워 수 - 1)
   */
  @Override
  public void decreaseFollowerCount(UUID userId) {
    decrementIfPresent(followerKey(userId));    // 팔로우 취소 시 -1
  }

  /**
   * 해당 유저가 다른 사람을 새로 팔로우했을 때 -> (캐시에 저장된 팔로잉 수 + 1)
   */
  @Override
  public void increaseFollowingCount(UUID userId) {
    incrementIfPresent(followingKey(userId));
  }

  /**
   * 해당 유저에 다른 사람에 대한 팔로우를 취소했을 때 (캐시에 저장된 팔로잉 수 - 1)
   */
  @Override
  public void decreaseFollowingCount(UUID userId) {
    decrementIfPresent(followingKey(userId));
  }

  // ---- 헬퍼 메서드 ----

  /**
   * Redis에 key에 해당하는 카운트 값 읽어옴
   */
  private Optional<Long> getCount(String key) {

    try {

      String cached = stringRedisTemplate.opsForValue().get(key);

      // Redis에 키가 없을 때
      if (cached == null) {
        return Optional.empty();
      }

      log.debug("[Cache] HIT: key={}", key);

      return Optional.of(Long.parseLong(cached));

    } catch (Exception e) {

      // Redis 연결 실패
      // 조회가 실패하면 예외 발생 x, 캐시 미스인 것처럼 처리
      log.warn("[Cache] Redis 조회 실패, fail: key={}, error={}", key, e.getMessage());

      return Optional.empty();
    }
  }

  /**
   * DB에서 새로 계산한 count 값을 Redis에 저장 및 TTL 설정
   */
  private void saveCount(String key, long count) {

    try {

      stringRedisTemplate.opsForValue().set(key, String.valueOf(count), COUNT_TTL);

      log.debug("[Cache] 저장: key={}, value={}", key, count);

    } catch (Exception e) {

      log.warn("[Cache] Redis 저장 실패, 무시: key={}, error={}", key, e.getMessage());
    }
  }

  /**
   *  캐시에 저장된 값이 있을 때만 1 증가
   */
  private void incrementIfPresent(String key) {

    try {

      if (stringRedisTemplate.hasKey(key)) {

        // Redis의 INCR -> 현재 저장된 숫자에 + 1
        stringRedisTemplate.opsForValue().increment(key);

        log.debug("[Cache] INCR: key={}", key);
      }

    } catch (Exception e) {

      log.warn("[Cache] Redis 값 증가 실패, 무시: key={}, error={}", key, e.getMessage());
    }
  }

  /**
   *  캐시에 저장된 값이 있을 때만 1 감소
   */
  private void decrementIfPresent(String key) {

    try {

      if (stringRedisTemplate.hasKey(key)) {

        stringRedisTemplate.opsForValue().decrement(key);

        log.debug("[Cache] DECR: key={}", key);
      }

    } catch (Exception e) {

      log.warn("[Cache] Redis 값 감소 실패, 무시: key={}, error={}", key, e.getMessage());
    }
  }

  /**
   * 들어온 userId에 대한 Redis 키 이름 문자열로 생성
   */
  private String followerKey(UUID userId) {
    return FOLLOWER_KEY_PREFIX + userId; // 예: "follow:followerCount:3f2504e0-..."
  }

  private String followingKey(UUID userId) {
    return FOLLOWING_KEY_PREFIX + userId; // 예: "follow:followingCount:3f2504e0-..."
  }
}
