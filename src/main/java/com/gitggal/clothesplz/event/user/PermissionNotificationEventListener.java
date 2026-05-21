package com.gitggal.clothesplz.event.user;

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
public class PermissionNotificationEventListener {

  private final NotificationService notificationService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handlePermissionChanged(PermissionChangedEvent event) {

    notificationService.send(new NotificationRequest(
        event.userId(),
        "권한이 변경되었습니다.",
        "회원 권한이 " + event.newRole().name() + "(으)로 변경되었습니다.",
        NotificationLevel.WARNING
    ));
  }
}
