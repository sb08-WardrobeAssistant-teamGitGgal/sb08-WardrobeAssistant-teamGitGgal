package com.gitggal.clothesplz.controller.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gitggal.clothesplz.controller.ControllerTestSupport;
import com.gitggal.clothesplz.repository.notification.SseEmitterRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@DisplayName("SSE 컨트롤러 테스트")
class SseControllerTest extends ControllerTestSupport {

  @Autowired
  private SseEmitterRepository emitterRepository;

  // ─── GET /api/sse ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("SSE 구독 요청 시 비동기 연결이 시작되고 text/event-stream 응답을 반환한다")
  void subscribe_validRequest_asyncStartedWithEventStreamContentType() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    // when
    MvcResult mvcResult = mockMvc.perform(get("/api/sse")
            .param("userId", userId.toString())
            .accept(MediaType.TEXT_EVENT_STREAM))
        .andExpect(request().asyncStarted())
        .andReturn();

    // then - asyncDispatch 없이 응답 헤더만 확인 (dispatch하면 30분 타임아웃까지 블로킹됨)
    assertThat(mvcResult.getResponse().getContentType())
        .contains(MediaType.TEXT_EVENT_STREAM_VALUE);
  }

  @Test
  @DisplayName("SSE 구독 후 해당 userId의 emitter가 저장소에 등록된다")
  void subscribe_afterConnect_emitterIsRegistered() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    // when
    mockMvc.perform(get("/api/sse")
            .param("userId", userId.toString())
            .accept(MediaType.TEXT_EVENT_STREAM))
        .andExpect(request().asyncStarted());

    // then
    assertThat(emitterRepository.findByUserId(userId)).isPresent();
  }

  @Test
  @DisplayName("userId 파라미터 없이 요청하면 400을 반환한다")
  void subscribe_missingUserId_returns400() throws Exception {
    mockMvc.perform(get("/api/sse")
            .accept(MediaType.TEXT_EVENT_STREAM))
        .andExpect(status().isBadRequest());
  }
}
