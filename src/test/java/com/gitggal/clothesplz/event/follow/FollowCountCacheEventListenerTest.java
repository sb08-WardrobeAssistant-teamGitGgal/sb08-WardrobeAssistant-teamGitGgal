package com.gitggal.clothesplz.event.follow;

import static org.mockito.BDDMockito.then;

import com.gitggal.clothesplz.service.follow.FollowCountCacheService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("팔로우 카운트 캐시 이벤트 리스너 테스트")
class FollowCountCacheEventListenerTest {

  @Mock
  private FollowCountCacheService followCountCacheService;

  @InjectMocks
  private FollowCountCacheEventListener listener;

  @Test
  @DisplayName("팔로우 생성 이벤트 수신 시 followee의 팔로워 수와 follower의 팔로잉 수를 증가시킨다.")
  void handleFollowCreated_increasesFollowerAndFollowingCount() {

    // given
    UUID followerId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();
    FollowCreatedEvent event = new FollowCreatedEvent(followerId, followeeId, "팔로워A");

    // when
    listener.handleFollowCreated(event);

    // then
    then(followCountCacheService).should().increaseFollowerCount(followeeId);
    then(followCountCacheService).should().increaseFollowingCount(followerId);
  }

  @Test
  @DisplayName("팔로우 취소 이벤트 수신 시 followee의 팔로워 수와 follower의 팔로잉 수를 감소시킨다.")
  void handleFollowCancelled_decreasesFollowerAndFollowingCount() {

    // given
    UUID followerId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();
    FollowCancelledEvent event = new FollowCancelledEvent(followerId, followeeId);

    // when
    listener.handleFollowCancelled(event);

    // then
    then(followCountCacheService).should().decreaseFollowerCount(followeeId);
    then(followCountCacheService).should().decreaseFollowingCount(followerId);
  }
}
