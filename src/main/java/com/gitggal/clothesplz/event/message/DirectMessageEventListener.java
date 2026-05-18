package com.gitggal.clothesplz.event.message;

import com.gitggal.clothesplz.dto.notification.NotificationRequest;
import com.gitggal.clothesplz.entity.notification.NotificationLevel;
import com.gitggal.clothesplz.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DirectMessageEventListener {

  private final SimpMessagingTemplate messagingTemplate;
  private final NotificationService notificationService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleDirectMessageSent(DirectMessageSentEvent event) {
    messagingTemplate.convertAndSend(event.destination(), event.dto());

    notificationService.send(new NotificationRequest(
        event.receiverId(),
        "[DM] " + event.senderName(),
        event.dto().content(),
        NotificationLevel.INFO
    ));
  }
}
