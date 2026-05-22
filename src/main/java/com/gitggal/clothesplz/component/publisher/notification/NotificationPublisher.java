package com.gitggal.clothesplz.component.publisher.notification;

import com.gitggal.clothesplz.dto.notification.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

/**
 * Redis 채널에 메시지 발행 (알림)
 */
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

  private final RedisTemplate<String, Object> redisTemplate;
  private final ChannelTopic notificationTopic;

  // Redis 쪽으로 넘김
  public void publish(NotificationDto dto) {
    redisTemplate.convertAndSend(notificationTopic.getTopic(), dto);
  }
}
