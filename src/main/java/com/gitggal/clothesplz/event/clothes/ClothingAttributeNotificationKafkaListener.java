package com.gitggal.clothesplz.event.clothes;

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
public class ClothingAttributeNotificationKafkaListener {

  private final NotificationService notificationService;

  private final ObjectMapper objectMapper;

  @RetryableTopic(
      attempts = "5",

      backoff = @Backoff(delay = 1000, multiplier = 2.0),

      kafkaTemplate = "kafkaTemplate",

      exclude = {PayloadDeserializationException.class}
  )
  @KafkaListener(topics = "clothing-attribute-notification", groupId = "clothesplz-group")
  public void onClothingAttributeChanged(String payload) {

    try {
      ClothingAttributeChangedEvent event = objectMapper.readValue(payload,
          ClothingAttributeChangedEvent.class);

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

      notificationService.send(new NotificationRequest(
          event.userId(), title, content, NotificationLevel.INFO
      ));

    } catch (JsonProcessingException e) {
      log.error("[ClothingAttr Kafka Consumer] 역직렬화 실패 - payload={}", payload, e);
      throw new PayloadDeserializationException(payload, e);
    }
  }

  @DltHandler
  public void handleDlt(String payload, Exception e) {
    log.error("[ClothingAttr DLT] 최종 실패 - payload={}, error={}", payload, e.getMessage());
  }
}
