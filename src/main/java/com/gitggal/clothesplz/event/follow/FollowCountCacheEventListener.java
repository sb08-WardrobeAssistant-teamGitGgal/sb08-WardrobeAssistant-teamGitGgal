package com.gitggal.clothesplz.event.follow;

import com.gitggal.clothesplz.service.follow.FollowCountCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 팔로우 카운트 Redis 캐시 갱신 리스너
 * 트랜잭션 커밋 이후에만 실행되어 DB와 Redis 정합성 보장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FollowCountCacheEventListener {

  private final FollowCountCacheService followCountCacheService;

  /**
   * 팔로우 생성 커밋 후 캐시 반영
   */
  @Async("taskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleFollowCreated(FollowCreatedEvent event) {

    log.info("[Cache] 팔로우 생성 캐시 반영: followerId={}, followeeId={}", event.followerId(), event.followeeId());

    followCountCacheService.increaseFollowerCount(event.followeeId());

    followCountCacheService.increaseFollowingCount(event.followerId());
  }

  /**
   * 팔로우 취소 커밋 후 캐시 반영
   */
  @Async("taskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleFollowCancelled(FollowCancelledEvent event) {

    log.info("[Cache] 팔로우 취소 캐시 반영: followerId={}, followeeId={}", event.followerId(), event.followeeId());

    followCountCacheService.decreaseFollowerCount(event.followeeId());

    followCountCacheService.decreaseFollowingCount(event.followerId());
  }
}
