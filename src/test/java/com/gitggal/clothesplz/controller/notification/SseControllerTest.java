package com.gitggal.clothesplz.controller.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

import com.gitggal.clothesplz.config.TestSecurityConfig;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.user.UserRole;
import com.gitggal.clothesplz.exception.GlobalExceptionHandler;
import com.gitggal.clothesplz.repository.notification.SseEmitterRepository;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import com.gitggal.clothesplz.security.jwt.JwtAuthenticationFilter;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
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

  private ClothesUserDetails mockUserDetails(UUID userId) {
    UserDto userDto =
        new UserDto(userId, Instant.now(), "test@test.com", "테스터", UserRole.USER, false);
    return new ClothesUserDetails(userDto, "password1234!");
  }

  @Test
  @DisplayName("SSE 구독 요청 시 비동기 연결이 시작되고 text/event-stream 응답을 반환한다")
  void subscribe_validRequest_asyncStartedWithEventStreamContentType() throws Exception {

    // given
    UUID userId = UUID.randomUUID();

    // when
    MvcResult mvcResult = mockMvc.perform(get("/api/sse")
            .accept(MediaType.TEXT_EVENT_STREAM)
            .with(user(mockUserDetails(userId)))
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
            .accept(MediaType.TEXT_EVENT_STREAM)
            .with(user(mockUserDetails(userId)))
            .with(csrf()))
        .andExpect(request().asyncStarted());

    // then
    verify(emitterRepository).save(eq(userId), any(SseEmitter.class));
  }

  @Test
  @DisplayName("연결 종료(onCompletion) 콜백이 실행되면 저장소에서 삭제된다")
  void onCompletion_callback_executes_delete() throws Exception {

    // given
    UUID userId = UUID.randomUUID();
    ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);

    try (MockedConstruction<SseEmitter> mocked = mockConstruction(SseEmitter.class)) {
      // when
      mockMvc.perform(get("/api/sse")
              .with(user(mockUserDetails(userId)))
              .with(csrf()))
          .andExpect(request().asyncStarted());

      SseEmitter mockEmitter = mocked.constructed().get(0);

      // then
      verify(mockEmitter).onCompletion(callbackCaptor.capture());

      callbackCaptor.getValue().run();

      verify(emitterRepository).deleteByUserId(userId);
    }
  }

  @Test
  @DisplayName("초기 연결 이벤트 전송 실패 시 completeWithError가 호출된다")
  void subscribe_sendFailure_callsCompleteWithError() throws Exception {

    // given
    UUID userId = UUID.randomUUID();

    try (MockedConstruction<SseEmitter> mocked = mockConstruction(SseEmitter.class,
        (mock, context) -> {
          doThrow(new IOException("연결 실패"))
              .when(mock).send(any(SseEmitter.SseEventBuilder.class));
        })) {

      // when
      mockMvc.perform(get("/api/sse")
              .with(user(mockUserDetails(userId)))
              .with(csrf()))
          .andExpect(request().asyncStarted());

      // then
      SseEmitter mockEmitter = mocked.constructed().get(0);

      verify(mockEmitter).completeWithError(any(IOException.class));
    }
  }

  @Test
  @DisplayName("타임아웃(onTimeout) 콜백이 실행되면 저장소에서 삭제된다")
  void onTimeout_callback_executes_delete() throws Exception {

    // given
    UUID userId = UUID.randomUUID();
    ArgumentCaptor<Runnable> timeoutCaptor = ArgumentCaptor.forClass(Runnable.class);

    try (MockedConstruction<SseEmitter> mocked = mockConstruction(SseEmitter.class)) {
      // when
      mockMvc.perform(get("/api/sse")
              .with(user(mockUserDetails(userId)))
              .with(csrf()))
          .andExpect(request().asyncStarted());

      SseEmitter mockEmitter = mocked.constructed().get(0);

      // then
      verify(mockEmitter).onTimeout(timeoutCaptor.capture());

      timeoutCaptor.getValue().run();

      verify(emitterRepository).deleteByUserId(userId);
    }
  }

  @Test
  @DisplayName("연결 오류(onError) 콜백이 실행되면 저장소에서 삭제된다")
  void onError_callback_executes_delete() throws Exception {

    // given
    UUID userId = UUID.randomUUID();

    ArgumentCaptor<Consumer<Throwable>> errorCaptor =
        ArgumentCaptor.forClass((Class) Consumer.class);

    try (MockedConstruction<SseEmitter> mocked = mockConstruction(SseEmitter.class)) {
      // when
      mockMvc.perform(get("/api/sse")
              .with(user(mockUserDetails(userId)))
              .with(csrf()))
          .andExpect(request().asyncStarted());

      SseEmitter mockEmitter = mocked.constructed().get(0);

      verify(mockEmitter).onError(errorCaptor.capture());

      errorCaptor.getValue().accept(new RuntimeException("SSE Error"));

      verify(emitterRepository).deleteByUserId(userId);
    }
  }
}
