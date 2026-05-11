package com.gitggal.clothesplz.controller.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gitggal.clothesplz.config.TestSecurityConfig;
import com.gitggal.clothesplz.exception.GlobalExceptionHandler;
import com.gitggal.clothesplz.repository.notification.SseEmitterRepository;
import com.gitggal.clothesplz.security.jwt.JwtAuthenticationFilter;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Import({
    GlobalExceptionHandler.class,
    TestSecurityConfig.class
})
@WebMvcTest(
    controllers = SseController.class,
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class
        )
    }
)
@DisplayName("SSE 컨트롤러 테스트")
class SseControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
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
            .accept(MediaType.TEXT_EVENT_STREAM)
            .with(csrf()))
        .andExpect(request().asyncStarted())
        .andReturn();

    // then
    assertThat(mvcResult.getResponse().getContentType())
        .contains(MediaType.TEXT_EVENT_STREAM_VALUE);
  }

  @Test
  @DisplayName("SSE 구독 시 해당 userId로 Emitter가 저장소에 등록(save)되는지 검증한다")
  void subscribe_afterConnect_emitterIsRegistered() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    given(emitterRepository.findByUserId(userId))
        .willReturn(Optional.of(new SseEmitter()));

    // when
    mockMvc.perform(get("/api/sse")
            .param("userId", userId.toString())
            .accept(MediaType.TEXT_EVENT_STREAM)
            .with(csrf()))
        .andExpect(request().asyncStarted());

    // then
    verify(emitterRepository).save(any(UUID.class), any(SseEmitter.class));
  }

  @Test
  @DisplayName("userId 파라미터 없이 요청하면 400을 반환한다")
  void subscribe_missingUserId_returns400() throws Exception {
    mockMvc.perform(get("/api/sse")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .with(csrf()))
        .andExpect(status().isBadRequest());
  }
}
