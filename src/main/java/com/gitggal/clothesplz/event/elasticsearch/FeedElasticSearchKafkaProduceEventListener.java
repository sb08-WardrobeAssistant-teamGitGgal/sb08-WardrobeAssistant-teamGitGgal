package com.gitggal.clothesplz.event.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeedElasticSearchKafkaProduceEventListener {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @Async("taskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onSync(FeedElasticSearchSyncEvent event) {
    try {
      String payload = objectMapper.writeValueAsString(event);
      kafkaTemplate.send("feed-es-sync", event.feedId().toString(), payload)
          .whenComplete((result, e) -> {
            if (e != null) {
              log.error("[ES Kafka] es 동기화 메시지 전송 최종 실패 (재시도 모두 소진) - feedId={}, error={}",
                  event.feedId(), e.getMessage());
            }
          });
    } catch (JsonProcessingException e) {
      log.error("[ES Kafka] 직렬화 실패 - feedId={}", event.feedId(), e);
    }
  }

  @Async("taskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onDelete(FeedElasticSearchDeleteEvent event) {
    try {
      String payload = objectMapper.writeValueAsString(event);
      kafkaTemplate.send("feed-es-delete", event.feedId().toString(), payload)
          .whenComplete((result, e) -> {
            if (e != null) {
              log.error("[ES Kafka] es 삭제 메시지 전송 최종 실패 (재시도 모두 소진) - feedId={}, error={}",
                  event.feedId(), e.getMessage());
            }
          });
    } catch (JsonProcessingException e) {
      log.error("[ES Kafka] 직렬화 실패 - feedId={}", event.feedId(), e);
    }
  }
}
