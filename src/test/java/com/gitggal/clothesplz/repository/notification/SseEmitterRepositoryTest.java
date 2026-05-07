package com.gitggal.clothesplz.repository.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@DisplayName("SSE Emitter 레포지토리 테스트")
class SseEmitterRepositoryTest {

  private SseEmitterRepository emitterRepository;

  @BeforeEach
  void setUp() {
    emitterRepository = new SseEmitterRepository();
  }

  @Test
  @DisplayName("save 후 findByUserId로 저장된 emitter를 조회할 수 있다")
  void save_thenFindByUserId_returnsEmitter() {
    // given
    UUID userId = UUID.randomUUID();
    SseEmitter emitter = new SseEmitter();

    // when
    emitterRepository.save(userId, emitter);

    // then
    assertThat(emitterRepository.findByUserId(userId)).contains(emitter);
  }

  @Test
  @DisplayName("등록되지 않은 userId로 조회하면 empty를 반환한다")
  void findByUserId_unregisteredUser_returnsEmpty() {
    assertThat(emitterRepository.findByUserId(UUID.randomUUID())).isEmpty();
  }

  @Test
  @DisplayName("deleteByUserId 호출 후 해당 emitter가 제거된다")
  void deleteByUserId_removesEmitter() {
    // given
    UUID userId = UUID.randomUUID();
    emitterRepository.save(userId, new SseEmitter());

    // when
    emitterRepository.deleteByUserId(userId);

    // then
    assertThat(emitterRepository.findByUserId(userId)).isEmpty();
  }

  @Test
  @DisplayName("count는 현재 저장된 emitter 수를 반환한다")
  void count_returnsNumberOfStoredEmitters() {
    // given
    emitterRepository.save(UUID.randomUUID(), new SseEmitter());
    emitterRepository.save(UUID.randomUUID(), new SseEmitter());

    // when & then
    assertThat(emitterRepository.count()).isEqualTo(2);
  }

  @Test
  @DisplayName("deleteByUserId 후 count가 감소한다")
  void count_afterDelete_decreases() {
    // given
    UUID userId = UUID.randomUUID();
    emitterRepository.save(userId, new SseEmitter());
    emitterRepository.save(UUID.randomUUID(), new SseEmitter());

    // when
    emitterRepository.deleteByUserId(userId);

    // then
    assertThat(emitterRepository.count()).isEqualTo(1);
  }
}
