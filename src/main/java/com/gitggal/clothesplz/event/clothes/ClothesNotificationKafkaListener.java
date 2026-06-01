package com.gitggal.clothesplz.event.clothes;

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
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClothesNotificationKafkaListener {

  private final NotificationService notificationService;

  private final ObjectMapper objectMapper;

  @RetryableTopic(
      attempts = "5",
      backoff = @Backoff(delay = 1000, multiplier = 2.0),
      kafkaTemplate = "kafkaTemplate"
  )
  @KafkaListener(topics = "clothes-notification", groupId = "clothesplz-group")
  public void onClothesChanged(String payload) {

    try {

      ClothesChangedEvent event = objectMapper.readValue(payload, ClothesChangedEvent.class);

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

      notificationService.send(new NotificationRequest(
          event.userId(),

          title,

          content,

          NotificationLevel.INFO
      ));
    } catch (JsonProcessingException e) {
      log.error("[Clothes Kafka Consumer] 역직렬화 실패 - payload={}", payload, e);
      throw new RuntimeException(e);
    }
  }

  @DltHandler
  public void handleDlt(String payload, Exception e) {
    log.error("[Clothes DLT] 최종 실패 - payload={}, error={}", payload, e.getMessage());
  }
}
