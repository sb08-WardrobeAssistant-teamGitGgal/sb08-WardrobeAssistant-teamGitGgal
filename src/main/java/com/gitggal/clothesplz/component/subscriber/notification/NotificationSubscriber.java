package com.gitggal.clothesplz.component.subscriber.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.dto.notification.NotificationDto;
import com.gitggal.clothesplz.repository.notification.SseEmitterRepository;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Redis 채널에서 메시지 받아 Emitter로 전달하는 클래스
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSubscriber implements MessageListener {

  private final SseEmitterRepository emitterRepository;
  private final ObjectMapper objectMapper;

  @Override
  public void onMessage(Message message, @Nullable byte[] pattern) {

    try {

      NotificationDto dto = objectMapper.readValue(
          message.getBody(), NotificationDto.class
      );

      emitterRepository
          .findByUserId(dto.receiverId())
          .ifPresent(emitter -> {
            try {
              emitter.send(
                  SseEmitter.event()
                      .id(dto.id().toString())
                      .name("notifications")
                      .data(dto)
              );
            } catch (IOException e) {
              emitterRepository.deleteByUserId(dto.receiverId());
              log.warn("[Subscriber] SSE 전송 실패: receiverId={}", dto.receiverId());
            }
          });
    } catch (Exception e) {
      log.warn("[Subscriber] 알림 처리 실패", e);
    }
  }
}
