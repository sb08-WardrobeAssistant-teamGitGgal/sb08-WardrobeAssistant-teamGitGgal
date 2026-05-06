package com.gitggal.clothesplz.service.notification;

import com.gitggal.clothesplz.dto.notification.NotificationDto;
import com.gitggal.clothesplz.repository.notification.SseEmitterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService{

  private final SseEmitterRepository emitterRepository;

  /**
   * 알림 전송 메서드
   *
   * 동작
   * - receiverId로 저장소에서 SseEmitter 조회
   * - emitter가 있으면 (현재 SSE 연결 중) -> send
   * - emitter가 없으면 (오프라인)         -> 아무것도 안 함
   * @param dto - 전송할 알림 데이터
   */
  @Override
  public void send(NotificationDto dto) {
    emitterRepository
        .findByUserId(dto.receiverId())
        .ifPresent(emitter -> sendToEmitter(emitter, dto));
  }

  /**
   * 실제 SseEmitter에 이벤트 전송 - 전송 실패 (저장소에서 제거)
   */
  private void sendToEmitter(SseEmitter emitter, NotificationDto dto) {
  }
}
