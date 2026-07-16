package com.gitggal.clothesplz.service.follow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.gitggal.clothesplz.dto.follow.FollowCreateRequest;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.repository.follow.FollowRepository;
import com.gitggal.clothesplz.repository.profile.ProfileRepository;
import com.gitggal.clothesplz.repository.user.UserRepository;
import com.gitggal.clothesplz.support.IntegrationTestSupport;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * 팔로우 생성 트랜잭션 커밋/롤백에 따른 Redis 캐시 정합성 통합 테스트
 *
 * 캐시 증감 로직이 {@code @TransactionalEventListener(AFTER_COMMIT)}으로 분리되어 있기에
 * 이 테스트는 Mockito 목이 아닌 실제 Spring 트랜잭션 경계(커밋/롤백)를 직접 통과시켜야 의미가 있다.
 * 그래서 테스트 클래스 자체에는 {@code @Transactional}을 걸지 않는다 — 걸면 테스트가 끝날 때
 * 항상 롤백되어 AFTER_COMMIT 리스너가 영원히 실행되지 않기 때문이다.
 */
class FollowCountCacheConsistencyIntegrationTest extends IntegrationTestSupport {

  // profileRepository 호출 시점(= save 이후)에 강제로 예외를 던져 롤백 시나리오를 재현하기 위한 목
  @MockitoBean
  private ProfileRepository profileRepository;

  @Autowired
  private FollowService followService;

  @Autowired
  private FollowRepository followRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private FollowCountCacheService followCountCacheService;

  @Autowired
  private StringRedisTemplate stringRedisTemplate;

  private User follower;

  private User followee;

  private String followerCountKey;

  private String followingCountKey;

  @BeforeEach
  void setUp() {

    follower = userRepository.save(
        new User("follower", "follower-" + UUID.randomUUID() + "@test.com", "password"));

    followee = userRepository.save(
        new User("followee", "followee-" + UUID.randomUUID() + "@test.com", "password"));

    followerCountKey = "follow:followerCount:" + followee.getId();

    followingCountKey = "follow:followingCount:" + follower.getId();

    // incrementIfPresent/decrementIfPresent는 캐시에 값이 "있을 때만" 동작하므로
    // 증감이 실제로 반영되는지 보려면 기준값을 먼저 심어둬야 한다.
    followCountCacheService.saveFollowerCount(followee.getId(), 0L);
    followCountCacheService.saveFollowingCount(follower.getId(), 0L);

    given(profileRepository.findByUserIdIn(any())).willReturn(List.of());
  }

  @AfterEach
  void tearDown() {
    stringRedisTemplate.delete(followerCountKey);
    stringRedisTemplate.delete(followingCountKey);
  }

  @Test
  @DisplayName("팔로우 생성 트랜잭션이 커밋되면 AFTER_COMMIT 리스너가 Redis 캐시를 증가시킨다.")
  void createFollow_commit_incrementsCache() {

    // when
    followService.createFollow(new FollowCreateRequest(follower.getId(), followee.getId()));

    // then: @Async 리스너가 커밋 이후 비동기로 실행되므로 폴링으로 대기
    awaitCacheValue(followerCountKey, "1");
    awaitCacheValue(followingCountKey, "1");
  }

  @Test
  @DisplayName("팔로우 생성 트랜잭션이 롤백되면 Redis 캐시는 변경되지 않는다.")
  void createFollow_rollback_doesNotChangeCache() {

    // given: save() 이후 실행되는 profileRepository 호출에서 강제로 예외 발생 -> 트랜잭션 롤백 유도
    given(profileRepository.findByUserIdIn(any()))
        .willThrow(new RuntimeException("강제 실패 - 롤백 유도"));

    // when
    assertThatThrownBy(() ->
        followService.createFollow(new FollowCreateRequest(follower.getId(), followee.getId())))
        .isInstanceOf(RuntimeException.class);

    // then: DB에도 팔로우 row가 남아있지 않아야 한다 (트랜잭션 롤백 확인)
    assertThat(
        followRepository.existsByFollower_IdAndFollowee_Id(follower.getId(), followee.getId()))
        .isFalse();

    // 비동기 리스너가 실행될 시간을 충분히 준 뒤에도 캐시 값이 기준값(0) 그대로인지 확인
    sleepBriefly();
    assertThat(stringRedisTemplate.opsForValue().get(followerCountKey)).isEqualTo("0");
    assertThat(stringRedisTemplate.opsForValue().get(followingCountKey)).isEqualTo("0");
  }

  private void awaitCacheValue(String key, String expected) {

    long deadline = System.currentTimeMillis() + 2000;

    String actual = stringRedisTemplate.opsForValue().get(key);

    while (System.currentTimeMillis() < deadline && !expected.equals(actual)) {

      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      actual = stringRedisTemplate.opsForValue().get(key);
    }

    assertThat(actual).isEqualTo(expected);
  }

  private void sleepBriefly() {
    try {
      Thread.sleep(300);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
