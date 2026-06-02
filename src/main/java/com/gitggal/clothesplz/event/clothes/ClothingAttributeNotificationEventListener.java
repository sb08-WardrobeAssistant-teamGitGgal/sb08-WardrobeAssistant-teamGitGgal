package com.gitggal.clothesplz.event.clothes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class ClothingAttributeNotificationEventListener {

  private final KafkaTemplate<String, String> kafkaTemplate;

  private final ObjectMapper objectMapper;

  @Async("taskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleClothingAttributeChanged(ClothingAttributeChangedEvent event) {

    log.info("[ClothingAttr Kafka Producer] 메시지 전송 시작: userId={}", event.userId());

    try {

      String payload = objectMapper.writeValueAsString(event);

      kafkaTemplate.send("clothing-attribute-notification", event.userId().toString(), payload)
          .whenComplete((result, e) -> {
            if (e != null) {
              log.error("[ClothingAttr Kafka Producer] 전송 실패 - userId={}, error={}", event.userId(), e.getMessage());
            } else {
              log.info("[ClothingAttr Kafka Producer] 전송 성공: userId={}", event.userId());
            }
          });
    } catch (JsonProcessingException e) {
      log.error("[ClothingAttr Kafka Producer] 직렬화 실패 - userId={}", event.userId(), e);
    }

  }
}
