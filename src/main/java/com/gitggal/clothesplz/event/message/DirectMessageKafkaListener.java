package com.gitggal.clothesplz.event.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.dto.notification.NotificationRequest;
import com.gitggal.clothesplz.entity.notification.NotificationLevel;
import com.gitggal.clothesplz.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectMessageKafkaListener {

  // DB 알림 저장 + Redis Pub/Sub -> SSE 푸시 담당
  private final NotificationService notificationService;

  private final SimpMessagingTemplate messagingTemplate;

  private final ObjectMapper objectMapper;

  @RetryableTopic(
      attempts = "5",

      backoff = @Backoff(delay = 1000, multiplier = 2.0),

      kafkaTemplate = "kafkaTemplate"
  )
  @KafkaListener(topics = "dm-notification", groupId = "clothesplz-group")
  public void onDirectMessageNotification(String payload) {

    try {
      DirectMessageSentEvent event =
          objectMapper.readValue(payload, DirectMessageSentEvent.class);

      try {
        notificationService.send(new NotificationRequest(
            event.receiverId(),
            "[DM] " + event.senderName(),
            event.dto().content(),
            NotificationLevel.INFO
        ));
        log.info("[DM Kafka Consumer] 알림 저장 완료: receiverId={}", event.receiverId());
      } catch (Exception e) {
        log.error("[DM Kafka Consumer] 알림 저장 실패: receiverId={}", event.receiverId(), e);
      }

      try {
        messagingTemplate.convertAndSend(event.destination(), event.dto());
        log.info("[DM Kafka Consumer] STOMP 푸시 성공: destination={}", event.destination());
      } catch (Exception e) {
        log.error("[DM Kafka Consumer] STOMP 푸시 실패: destination={}", event.destination(), e);
      }

    } catch (JsonProcessingException e) {
      log.error("[DM Kafka Consumer] 역직렬화 실패 - payload={}", payload, e);
      throw new RuntimeException(e);
    }
  }

  @DltHandler
  public void handleDlt(String payload, Exception e) {
    log.error("[DM DLT] 최종 실패 - payload={}, error={}", payload, e.getMessage());
  }
}