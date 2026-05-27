package com.gitggal.clothesplz.event.clothes;

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
public class ClothesNotificationEventListener {

  private final NotificationService notificationService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleClothesChanged(ClothesChangedEvent event) {
    String title = switch (event.changeType()) {
      case CREATED -> "의상이 등록되었어요.";
      case UPDATED -> "의상 정보가 수정되었어요.";
      case DELETED -> "의상이 삭제되었어요.";
    };

    String content = switch (event.changeType()) {
      case CREATED -> "[" + event.clothesName() + "] 의상이 등록되었어요.";
      case UPDATED -> "[" + event.clothesName() + "] 의상 정보를 확인해보세요.";
      case DELETED -> "[" + event.clothesName() + "] 의상이 삭제되었어요.";
    };

    try {
      notificationService.send(new NotificationRequest(
          event.userId(),
          title,
          content,
          NotificationLevel.INFO
      ));
    } catch (Exception e) {
      log.warn("의상 변경 알림 전송 실패. userId={}", event.userId(), e);
    }
  }
}
