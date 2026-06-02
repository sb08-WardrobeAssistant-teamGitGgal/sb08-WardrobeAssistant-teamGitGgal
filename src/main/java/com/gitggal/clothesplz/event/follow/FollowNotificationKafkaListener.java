package com.gitggal.clothesplz.event.follow;

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
public class FollowNotificationKafkaListener {

  private final NotificationService notificationService;

  private final ObjectMapper objectMapper;

  /**
   * 재시도 로직
   *  - 최소 시도 1회 + 재시도 4회 = 총 5번 처리 시도.
   *  - 실패하면 -> DLT로 이동
   */
  @RetryableTopic(
      attempts = "5",

      backoff = @Backoff(delay = 1000, multiplier = 2.0),

      kafkaTemplate = "kafkaTemplate",

      exclude = {PayloadDeserializationException.class}
  )
  @KafkaListener(topics = "follow-notification", groupId = "clothesplz-group")
  public void onFollowNotification(String payload) {

    try {

      FollowCreatedEvent event = objectMapper.readValue(payload, FollowCreatedEvent.class);

      notificationService.send(new NotificationRequest(
          event.followeeId(),

          event.followerName() + "님이 나를 팔로우했어요.",

          null,

          NotificationLevel.INFO
      ));

    } catch (JsonProcessingException e) {
      log.error("[Follow Kafka] 역직렬화 실패 - payload={}", payload, e);
      throw new PayloadDeserializationException(payload, e);
    }
  }

  /**
   * 5번 재시도 모두 실패한 메시지 처리하는 메서드
   */
  @DltHandler
  public void handleDlt(String payload, Exception e) {
    log.error("[Follow DLT] 최종 실패 - payload={}, error={}", payload, e.getMessage());
  }
}
