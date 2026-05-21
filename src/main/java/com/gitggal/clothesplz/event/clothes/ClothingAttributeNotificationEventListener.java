package com.gitggal.clothesplz.event.clothes;

import com.gitggal.clothesplz.dto.notification.NotificationRequest;
import com.gitggal.clothesplz.entity.notification.NotificationLevel;
import com.gitggal.clothesplz.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ClothingAttributeNotificationEventListener {

  private final NotificationService notificationService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleClothingAttributeChanged(ClothingAttributeChangedEvent event) {

    String title = switch (event.changeType()) {
      case ADDED -> "새로운 의상 속성이 추가되었어요.";
      case UPDATED -> "의상 속성이 변경되었어요.";
      case DELETED -> "의상 속성이 삭제되었어요.";
    };

    String content = switch (event.changeType()) {
      case ADDED -> "내 의상에 [" + event.attributeName() + "] 속성을 추가해보세요.";
      case UPDATED -> "[" + event.attributeName() + "] 속성을 확인해보세요.";
      case DELETED -> "[" + event.attributeName() + "] 속성이 삭제되었어요.";
    };

    event.allUserIds().forEach(userId ->
        notificationService.send(new NotificationRequest(
            userId,
            title,
            content,
            NotificationLevel.INFO
        ))
    );
  }
}
