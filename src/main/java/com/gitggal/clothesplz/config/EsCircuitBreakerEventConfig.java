package com.gitggal.clothesplz.config;

import com.gitggal.clothesplz.event.elasticsearch.DltReplayService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class EsCircuitBreakerEventConfig {

  // 찾아올 CB의 이름
  private static final String CB_NAME = "esSearch";
  // CB를 찾아올 저장소
  private final CircuitBreakerRegistry circuitBreakerRegistry;
  // 실제로 pause/resume을 실행할 서비스
  private final DltReplayService dltReplayService;

  @PostConstruct
  public void registerStateTransitionListener() {
    // 저장소에서 CB 인스턴스 조회
    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(CB_NAME);

    circuitBreaker.getEventPublisher()
        // 상태가 전환됐을 때 반응하는 리스너
        .onStateTransition(event -> {
          // 전환된 결과 상태
          CircuitBreaker.State targetState =
              event.getStateTransition().getToState();

          // 결과 상태가 close(정상)일 경우
          // 즉, ES 복구됨 -> DLT 리스너 켜서 밀린 메시지 재발행 시작
          if (targetState == CircuitBreaker.State.CLOSED) {
            dltReplayService.resume();
          } else if (targetState == CircuitBreaker.State.OPEN) {
            // 결과 상태가 open(장애)일 경우
            // ES 죽음 -> DLT 리스너를 꺼서 재발행을 멈춤
            dltReplayService.pause();
          }
        });

    log.info("[CB Event] esSearch CircuitBreaker 상태 전환 리스너 등록 완료");
  }
}
