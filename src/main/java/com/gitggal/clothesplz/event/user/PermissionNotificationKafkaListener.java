package com.gitggal.clothesplz.event.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.dto.notification.NotificationRequest;
import com.gitggal.clothesplz.entity.notification.NotificationLevel;
import com.gitggal.clothesplz.event.PayloadDeserializationException;
import com.gitggal.clothesplz.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionNotificationKafkaListener {

  private final NotificationService notificationService;

  private final ObjectMapper objectMapper;

  @RetryableTopic(
      attempts = "5",

      backoff = @Backoff(delay = 1000, multiplier = 2.0),

      kafkaTemplate = "kafkaTemplate",

      exclude = {PayloadDeserializationException.class}
  )
  @KafkaListener(topics = "permission-notification", groupId = "clothesplz-group")
  public void onPermissionChanged(String payload) {

    try {
      PermissionChangedEvent event = objectMapper.readValue(payload, PermissionChangedEvent.class);

      notificationService.send(new NotificationRequest(
          event.userId(),
          "권한이 변경되었습니다.",
          "회원 권한이 " + event.newRole().name() + "(으)로 변경되었습니다.",
          NotificationLevel.WARNING
      ));
    } catch (JsonProcessingException e) {
      log.error("[Permission Kafka Consumer] 역직렬화 실패 - payload={}", payload, e);
      throw new PayloadDeserializationException(payload, e);
    }
  }

  @DltHandler
  public void handleDlt(Exception e) {
    log.error("[Permission DLT] 최종 실패 - error={}", e.getMessage());
  }
}
