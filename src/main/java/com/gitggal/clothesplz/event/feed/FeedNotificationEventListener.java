package com.gitggal.clothesplz.event.feed;

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
public class FeedNotificationEventListener {

  private final KafkaTemplate<String, String> kafkaTemplate;

  private final ObjectMapper objectMapper;

  @Async("taskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCommentCreated(FeedCommentCreatedEvent event) {

    try {

      String payload = objectMapper.writeValueAsString(event);

      kafkaTemplate.send("feed-comment-notification", event.feedOwnerId().toString(), payload)
          .whenComplete((result, e) -> {

            if (e != null) {
              log.error("[Feed Comment Kafka Producer] 전송 실패 - feedOwnerId={}, error={}",
                  event.feedOwnerId(), e.getMessage());
            } else {
              log.info("[Feed Comment Kafka Producer] 전송 성공: feedOwnerId={}", event.feedOwnerId());
            }
          });
    } catch (JsonProcessingException e) {
      log.error("[Feed Comment Kafka Producer] 직렬화 실패 - feedOwnerId={}", event.feedOwnerId(), e);
    }
  }

  @Async("taskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleFeedLiked(FeedLikedEvent event) {
    try {

      String payload = objectMapper.writeValueAsString(event);

      kafkaTemplate.send("feed-liked-notification", event.feedOwnerId().toString(), payload)
          .whenComplete((result, e) -> {

            if (e != null) {
              log.error("[Feed Liked Kafka Producer] 전송 실패 - feedOwnerId={}, error={}",
                  event.feedOwnerId(), e.getMessage());
            } else {
              log.info("[Feed Liked Kafka Producer] 전송 성공: feedOwnerId={}", event.feedOwnerId());
            }
          });
    } catch (JsonProcessingException e) {
      log.error("[Feed Liked Kafka Producer] 직렬화 실패 - feedOwnerId={}", event.feedOwnerId(), e);
    }
  }

  @Async("taskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleFeedCreated(FeedCreatedEvent event) {
    try {

      String payload = objectMapper.writeValueAsString(event);

      // followerIds가 List<UUID>라 단일 키 없음 → null키 = 카프카가 알아서 파티션 순서대로 배분(라운드로빈)
      kafkaTemplate.send("feed-created-notification", null, payload)
          .whenComplete((result, e) -> {

            if (e != null) {
              log.error("[Feed Created Kafka Producer] 전송 실패 - error={}", e.getMessage());
            } else {
              log.info("[Feed Created Kafka Producer] 전송 성공");
            }
          });
    } catch (JsonProcessingException e) {
      log.error("[Feed Created Kafka Producer] 직렬화 실패", e);
    }
  }
}
