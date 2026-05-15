package com.gitggal.clothesplz.controller.notification;

import com.gitggal.clothesplz.repository.notification.SseEmitterRepository;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 클라이언트의 SSE 연결 요청을 처리하는 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class SseController {

  private final SseEmitterRepository emitterRepository;

  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(@AuthenticationPrincipal ClothesUserDetails userDetails) {

    UUID userId = userDetails.getUserDto().id();

    log.info("[Controller] SSE 연결 요청 시작: userId={}", userId);

    // SseEmitter 생성 - 30분 타임아웃 설정 - 타임아웃이 지나면 자동 연결 종료
    SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

    // userId로 SseEmitter 저장소에 등록
    emitterRepository.save(userId, emitter);

    // 연결 종료 콜백 등록
    emitter.onCompletion(() -> {
      emitterRepository.deleteByUserId(userId);
      log.info("[Controller] SSE 연결 종료 (completion): userId={}", userId);
    });
    emitter.onTimeout(() -> {
      emitterRepository.deleteByUserId(userId);
      log.warn("[Controller] SSE 연결 타임아웃: userId={}", userId);
    });
    emitter.onError((e) -> {
      emitterRepository.deleteByUserId(userId);
      log.warn("[Controller] SSE 연결 오류: userId={}, error={}", userId, e.getMessage());
    });

    // 최초 연결 확인 이벤트 전송 - 연결 직후 데이터 안 보내면 일부 환경에서 연결 안 된 것으로 판단
    try {
      emitter.send(
          SseEmitter.event()
              .name("connect")
              .data("[GitGgal] SSE 연결이 완료되었습니다.")
      );
      log.info("[Controller] SSE 연결 요청 완료: userId={}", userId);
    } catch (IOException e) {
      // 연결 직후 예외 발생 -> emitter 종료 처리
      log.warn("[Controller] SSE 연결 요청 실패: 초기 이벤트 전송 오류: userId={}", userId);
      emitter.completeWithError(e);
    }

    // SseEmitter 반환 -> 스프링이 연결 유지하며 대기
    return emitter;
  }
}
