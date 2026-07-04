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
import org.springframework.kafka.core.KafkaTemplate;
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
  private final KafkaTemplate<String, String> kafkaTemplate;

  @RetryableTopic(attempts = "5", backoff = @Backoff(delay = 1000, multiplier = 2.0),
      kafkaTemplate = "kafkaTemplate",
      numPartitions = "${spring.kafka.topic.partitions:1}",
      replicationFactor = "${spring.kafka.topic.replicas:1}",
      exclude = {JsonProcessingException.class})
  @KafkaListener(topics = "feed-es-sync", groupId = "clothesplz-group")
  public void onSyncHandler(String payload) throws JsonProcessingException {
    try {
      FeedElasticSearchSyncEvent event = objectMapper.readValue(payload, FeedElasticSearchSyncEvent.class);

      feedSearchRepository.save(FeedDocument.builder()
          .id(event.feedId().toString())
          .content(event.content())
          .build());
    } catch (JsonProcessingException e) {
      log.error("[ES Kafka] 역직렬화 실패 - payload={}", payload, e);
      throw e;
    }
  }

  @RetryableTopic(attempts = "5", backoff = @Backoff(delay = 1000, multiplier = 2.0),
      kafkaTemplate = "kafkaTemplate",
      numPartitions = "${spring.kafka.topic.partitions:1}",
      replicationFactor = "${spring.kafka.topic.replicas:1}",
      exclude = {JsonProcessingException.class})
  @KafkaListener(topics = "feed-es-delete", groupId = "clothesplz-group")
  public void onDeleteHandler(String payload) throws JsonProcessingException {
    try {
      FeedElasticSearchDeleteEvent event = objectMapper.readValue(payload, FeedElasticSearchDeleteEvent.class);

      feedSearchRepository.deleteById(event.feedId().toString());
    } catch (JsonProcessingException e) {
      log.error("[ES Kafka] 역직렬화 실패 - payload={}", payload, e);
      throw e;
    }
  }

  @DltHandler
  public void handleDlt(String payload, Exception e, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
    log.error("[ES DLT] topic={}, payload={}", topic, payload, e);
  }

  @KafkaListener(
      id = "feedEsDltReplayListener",
      topics = {"feed-es-sync-dlt", "feed-es-delete-dlt"},
      groupId = "dlt-replay-group",
      autoStartup = "false"
  )
  public void onDltReplay(
      String payload,
      @Header(KafkaHeaders.DLT_ORIGINAL_TOPIC) String originalTopic
  ) {
    log.info("[ES DLT Replay] 원본 토픽으로 재발행 - originalTopic={}",
        originalTopic);
    kafkaTemplate.send(originalTopic, payload);
  }
}
