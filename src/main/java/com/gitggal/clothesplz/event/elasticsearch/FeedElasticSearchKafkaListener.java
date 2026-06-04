package com.gitggal.clothesplz.event.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.document.feed.FeedDocument;
import com.gitggal.clothesplz.repository.feed.FeedSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeedElasticSearchKafkaListener {

  private final FeedSearchRepository feedSearchRepository;
  private final ObjectMapper objectMapper;

  @RetryableTopic(attempts = "5", backoff = @Backoff(delay = 1000, multiplier = 2.0),
    kafkaTemplate = "kafkaTemplate")
  @KafkaListener(topics = "feed-es-sync", groupId = "clothesplz-group")
  public void onSyncHandler(String payload) {
    try {
      FeedElasticSearchSyncEvent event = objectMapper.readValue(payload, FeedElasticSearchSyncEvent.class);

      feedSearchRepository.save(FeedDocument.builder()
          .id(event.feedId().toString())
          .content(event.content())
          .authorId(event.authorId().toString())
          .skyStatus(event.skyStatus().name())
          .precipitationType(event.precipitationType().name())
          .likeCount(event.likeCount())
          .createdAt(event.createdAt())
          .build());
    } catch (JsonProcessingException e) {
      log.error("[ES Kafka] 역직렬화 실패 - payload={}", payload, e);
      throw new RuntimeException(e); // @RetryableTopic이 재시도하도록 예외 던짐
    }
  }

  @RetryableTopic(attempts = "5", backoff = @Backoff(delay = 1000, multiplier = 2.0),
      kafkaTemplate = "kafkaTemplate")
  @KafkaListener(topics = "feed-es-delete", groupId = "clothesplz-group")
  public void onDeleteHandler(String payload) {
    try {
      FeedElasticSearchDeleteEvent event = objectMapper.readValue(payload, FeedElasticSearchDeleteEvent.class);

      feedSearchRepository.deleteById(event.feedId().toString());
    } catch (JsonProcessingException e) {
      log.error("[ES Kafka] 역직렬화 실패 - payload={}", payload, e);
      throw new RuntimeException(e); // @RetryableTopic이 재시도하도록 예외 던짐
    }
  }

  @DltHandler
  public void handleDlt(String payload, Exception e, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
    log.error("[ES DLT] topic={}, payload={}", topic, payload, e);
  }
}
