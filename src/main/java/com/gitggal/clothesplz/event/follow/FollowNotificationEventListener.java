package com.gitggal.clothesplz.event.follow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.dto.notification.NotificationRequest;
import com.gitggal.clothesplz.entity.notification.NotificationLevel;
import com.gitggal.clothesplz.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowNotificationEventListener {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @Async("taskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleFollowCreated(FollowCreatedEvent event) {

    try {

      String payload = objectMapper.writeValueAsString(event);

      kafkaTemplate.send(
          "follow-notification",
          event.followeeId().toString(),
          payload
      ).whenComplete((result, e) -> {
        if (e != null) {
          log.error("[Follow Kafka] 메시지 전송 실패 - followeeId={}, error={}", event.followeeId(),
              e.getMessage());
        }
      });

    } catch (JsonProcessingException e) {
      log.error("[Follow Kafka] 직렬화 실패 - followeeId={}", event.followeeId(), e);
    }
  }
}
