package com.gitggal.clothesplz.event.elasticsearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DltReplayService {

  // registry에서 해당 컨테이너를 찾기 위함
  private static final String LISTENER_ID = "feedEsDltReplayListener";
  private final KafkaListenerEndpointRegistry registry;

  public void resume() {
    MessageListenerContainer container = registry.getListenerContainer(LISTENER_ID);
    if (container != null && !container.isRunning()) {
      container.resume();
      log.info("[DLT Replay] ES 복구 감지 - 리스너 재개");
    }
  }

  public void pause() {
    MessageListenerContainer container = registry.getListenerContainer(LISTENER_ID);
    if (container != null && container.isRunning()) {
      container.pause();
      log.info("[DLT Replay] ES 장애 감지 - 리스너 일시정지");
    }
  }
}
