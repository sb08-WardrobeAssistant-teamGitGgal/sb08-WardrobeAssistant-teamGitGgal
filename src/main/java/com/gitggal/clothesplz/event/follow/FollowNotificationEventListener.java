package com.gitggal.clothesplz.event.follow;

import com.gitggal.clothesplz.dto.notification.NotificationRequest;
import com.gitggal.clothesplz.entity.notification.NotificationLevel;
import com.gitggal.clothesplz.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowNotificationEventListener {

  private NotificationService notificationService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleFollowCreated(FollowCreatedEvent event) {

    try {
      notificationService.send(new NotificationRequest(
          event.followeeId(),
          event.followerName() + "님이 나를 팔로우했어요.",
          null,
          NotificationLevel.INFO
      ));
    } catch (Exception e) {
      log.warn("팔로우 알림 전송 실패. followeeId={}", event.followeeId(), e);
    }
  }
}
