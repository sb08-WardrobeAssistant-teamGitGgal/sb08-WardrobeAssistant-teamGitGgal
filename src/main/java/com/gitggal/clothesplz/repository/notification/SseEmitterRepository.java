package com.gitggal.clothesplz.repository.notification;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Repository
public class SseEmitterRepository {

  private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

  /**
   * 사용자의 SseEmitter를 저장
   * @param userId  - 사용자 ID
   * @param emitter - 해당 사용자와의 SSE 연결 객체
   */
  public void save(UUID userId, SseEmitter emitter) {
    emitters.put(userId, emitter);
  }

  /**
   * 사용자 ID로 SseEmitter 조회
   * @param userId  - 사용자 ID
   * @return SseEmitter - 연결 중이면 있음, 아니면 비워둔다.
   */
  public Optional<SseEmitter> findByUserId(UUID userId) {
    return Optional.ofNullable(emitters.get(userId));
  }

  /**
   * 사용자의 SseEmitter를 삭제
   * - 연결이 끊기거나 타임아웃 시 호출
   *
   * @param userId 사용자 ID
   */
  public void deleteByUserId(UUID userId) {
    emitters.remove(userId);
  }

  /**
   * 현재 연결된 사용자 수 (모니터링 용)
   */
  public int count() {
    return emitters.size();
  }

}
