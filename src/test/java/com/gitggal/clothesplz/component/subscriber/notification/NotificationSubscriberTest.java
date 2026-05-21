package com.gitggal.clothesplz.component.subscriber.notification;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.dto.notification.NotificationDto;
import com.gitggal.clothesplz.entity.notification.NotificationLevel;
import com.gitggal.clothesplz.repository.notification.SseEmitterRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationSubscriber 테스트")
class NotificationSubscriberTest {

  @Mock
  private SseEmitterRepository emitterRepository;
  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private NotificationSubscriber subscriber;

  private Message message;
  private NotificationDto dto;
  private UUID receiverId;

  @BeforeEach
  void setUp() {
    receiverId = UUID.randomUUID();
    dto = new NotificationDto(
        UUID.randomUUID(), Instant.now(), receiverId,
        "제목", "내용", NotificationLevel.INFO
    );
    message = mock(Message.class);
    given(message.getBody()).willReturn(new byte[0]);
  }

  @Test
  @DisplayName("emitter 존재 시 SSE 이벤트를 전송한다.")
  void onMessage_emitterFound_sendsSseEvent() throws Exception {

    // given
    SseEmitter emitter = mock(SseEmitter.class);
    given(objectMapper.readValue(any(byte[].class), eq(NotificationDto.class))).willReturn(dto);
    given(emitterRepository.findByUserId(receiverId)).willReturn(Optional.of(emitter));

    // when
    subscriber.onMessage(message, null);

    // then
    then(emitter).should().send(any(SseEmitter.SseEventBuilder.class));
  }

  @Test
  @DisplayName("emitter 없으면 아무것도 하지 않는다.")
  void onMessage_emitterNotFound_doesNothing() throws Exception {

    // given
    given(objectMapper.readValue(any(byte[].class), eq(NotificationDto.class))).willReturn(dto);
    given(emitterRepository.findByUserId(receiverId)).willReturn(Optional.empty());

    // when
    subscriber.onMessage(message, null);

    // then
    then(emitterRepository).should(never()).deleteByUserId(any());
  }

  @Test
  @DisplayName("SSE 전송 중 IOException 발생 시 emitter를 삭제한다.")
  void onMessage_ioExceptionOnSend_deletesEmitter() throws Exception {

    // given
    SseEmitter emitter = mock(SseEmitter.class);
    given(objectMapper.readValue(any(byte[].class), eq(NotificationDto.class))).willReturn(dto);
    given(emitterRepository.findByUserId(receiverId)).willReturn(Optional.of(emitter));
    willThrow(new IOException("전송 실패")).given(emitter).send(any(SseEmitter.SseEventBuilder.class));

    // when
    subscriber.onMessage(message, null);

    // then
    then(emitterRepository).should().deleteByUserId(receiverId);
  }

  @Test
  @DisplayName("JSON 역직렬화 실패 시 예외를 전파하지 않는다.")
  void onMessage_jsonParseFailure_doesNotThrow() throws Exception {

    // given
    given(objectMapper.readValue(any(byte[].class), eq(NotificationDto.class)))
        .willThrow(new JsonProcessingException("파싱 실패") {});

    // when / then
    assertThatNoException().isThrownBy(() -> subscriber.onMessage(message, null));
  }
}
