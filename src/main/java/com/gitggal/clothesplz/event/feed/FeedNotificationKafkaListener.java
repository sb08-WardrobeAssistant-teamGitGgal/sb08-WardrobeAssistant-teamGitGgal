package com.gitggal.clothesplz.event.feed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.dto.notification.NotificationRequest;
import com.gitggal.clothesplz.entity.notification.NotificationLevel;
import com.gitggal.clothesplz.service.notification.NotificationService;
import java.util.UUID;
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
public class FeedNotificationKafkaListener {

  private final NotificationService notificationService;

  private final ObjectMapper objectMapper;

  @RetryableTopic(
      attempts = "5",

      backoff = @Backoff(delay = 1000, multiplier = 2.0),

      kafkaTemplate = "kafkaTemplate"
  )
  @KafkaListener(topics = "feed-comment-notification", groupId = "clothesplz-group")
  public void onCommentCreated(String payload) {

    try {

      FeedCommentCreatedEvent event = objectMapper.readValue(payload,
          FeedCommentCreatedEvent.class);

      notificationService.send(new NotificationRequest(
          event.feedOwnerId(),

          event.commenterName() + "님이 댓글을 달았어요.",

          event.commentContent(),

          NotificationLevel.INFO
      ));
    } catch (JsonProcessingException e) {
      log.error("[Feed Comment Kafka Consumer] 역직렬화 실패 - payload={}", payload, e);
      throw new RuntimeException(e);
    }
  }

  @RetryableTopic(
      attempts = "5",

      backoff = @Backoff(delay = 1000, multiplier = 2.0),

      kafkaTemplate = "kafkaTemplate"
  )
  @KafkaListener(topics = "feed-liked-notification", groupId = "clothesplz-group")
  public void onFeedLiked(String payload) {
    try {

      FeedLikedEvent event = objectMapper.readValue(payload, FeedLikedEvent.class);

      notificationService.send(new NotificationRequest(
          event.feedOwnerId(),
          event.likerName() + "님이 내 피드를 좋아합니다.",
          event.feedContent(),
          NotificationLevel.INFO
      ));
    } catch (JsonProcessingException e) {
      log.error("[Feed Liked Kafka Consumer] 역직렬화 실패 - payload={}", payload, e);
      throw new RuntimeException(e);
    }
  }

  @RetryableTopic(
      attempts = "5",
      backoff = @Backoff(delay = 1000, multiplier = 2.0),
      kafkaTemplate = "kafkaTemplate"
  )
  @KafkaListener(topics = "feed-created-notification", groupId = "clothesplz-group")
  public void onFeedCreated(String payload) {
    try {

      FeedCreatedEvent event = objectMapper.readValue(payload, FeedCreatedEvent.class);

      // 팔로워 1명 실패해도 나머지 계속 처리
      for (UUID followerId : event.followerIds()) {
        try {
          notificationService.send(new NotificationRequest(
              followerId,
              event.authorName() + "님이 새로운 피드를 작성했어요.",
              event.feedContent(),
              NotificationLevel.INFO
          ));
        } catch (Exception e) {
          log.warn("[Feed Created Kafka Consumer] 팔로워 알림 실패 - followerId={}", followerId, e);
        }
      }
    } catch (JsonProcessingException e) {
      log.error("[Feed Created Kafka Consumer] 역직렬화 실패 - payload={}", payload, e);
      throw new RuntimeException(e);
    }
  }

  // 토픽 3개 중 어느 것이든 DLT 도달 시 처리
  @DltHandler
  public void handleDlt(String payload, Exception e) {
    log.error("[Feed DLT] 최종 실패 - payload={}, error={}", payload, e.getMessage());
  }
}
