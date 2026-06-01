package com.gitggal.clothesplz.event.message;

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
public class DirectMessageEventListener {

  private final KafkaTemplate<String, String> kafkaTemplate;

  private final ObjectMapper objectMapper;

  @Async("taskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleDirectMessageSent(DirectMessageSentEvent event) {

    log.info("[DM Kafka Producer] 메시지 전송 시작: receiverId={}", event.receiverId());

    try {

      String payload = objectMapper.writeValueAsString(event);

      kafkaTemplate.send(
          "dm-notification",
          event.receiverId().toString(),
          payload
      ).whenComplete((result, e) -> {
        if (e != null) {
          log.error("[DM Kafka Producer] 전송 실패 - receiverId={}", event.receiverId(), e);
        } else {
          log.info("[DM Kafka Producer] 전송 성공: receiverId={}", event.receiverId());
        }
      });

    } catch (JsonProcessingException e) {
      log.error("[DM Kafka Producer] 직렬화 실패 - receiverId={}", event.receiverId(), e);
    }
  }
}
