package com.gitggal.clothesplz.event.message;

import com.gitggal.clothesplz.dto.notification.NotificationRequest;
import com.gitggal.clothesplz.entity.notification.NotificationLevel;
import com.gitggal.clothesplz.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageEventListener {

  private final SimpMessagingTemplate messagingTemplate;
  private final NotificationService notificationService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleDirectMessageSent(DirectMessageSentEvent event) {

    log.info("[EventListener] DM 알림 이벤트 수신 시작: receiverId={}", event.receiverId());

    try {
      notificationService.send(new NotificationRequest(
          event.receiverId(),
          "[DM] " + event.senderName(),
          event.dto().content(),
          NotificationLevel.INFO
      ));

      log.info("[EventListener] 알림 DB 저장 및 Redis 발행 완료");

    } catch (Exception e) {
      log.error("[EventListener] 알림 생성 중 오류 발생: receiverId={}", event.receiverId(), e);
    }

    try {
      messagingTemplate.convertAndSend(event.destination(), event.dto());

      log.info("[EventListener] STOMP 푸시 전송 성공: destination={}", event.destination());
    } catch (Exception e) {
      log.error("[EventListener] STOMP 푸시 전송 실패: destination={}", event.destination(), e);
    }

  }
}
