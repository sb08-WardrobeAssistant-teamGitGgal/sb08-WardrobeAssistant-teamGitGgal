package com.gitggal.clothesplz.controller.message;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import com.gitggal.clothesplz.dto.message.DirectMessageCreateRequest;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.MessageErrorCode;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.security.websocket.StompAuthChannelInterceptor;
import com.gitggal.clothesplz.service.message.DirectMessageService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

@ExtendWith(MockitoExtension.class)
@DisplayName("DM WebSocket 컨트롤러 테스트")
public class DirectMessageWebSocketControllerTest {

  @Mock
  private DirectMessageService directMessageService;

  @InjectMocks
  private DirectMessageWebSocketController controller;

  @Test
  @DisplayName("DM 송신 시 서비스에 요청을 위임한다.")
  void sendDirectMessage_success() {
    // given
    UUID authUserId = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();

    DirectMessageCreateRequest request =
        new DirectMessageCreateRequest(authUserId, receiverId, "테스트 메시지");

    SimpMessageHeaderAccessor accessor = mock(SimpMessageHeaderAccessor.class);
    Map<String, Object> sessionAttrs = new HashMap<>();
    sessionAttrs.put(StompAuthChannelInterceptor.USER_ID_KEY, authUserId);

    given(accessor.getSessionAttributes()).willReturn(sessionAttrs);

    // when
    controller.sendDirectMessage(request, accessor);

    // then
    then(directMessageService).should().send(request, authUserId);
  }

  @Test
  @DisplayName("세션 속성이 null이면 JWT_TOKEN_INVALID 예외가 발생한다.")
  void sendDirectMessage_sessionNull_throwsBusinessException() {

    // given
    UUID senderId = UUID.randomUUID();
    DirectMessageCreateRequest request =
        new DirectMessageCreateRequest(senderId, UUID.randomUUID(), "테스트 메시지");

    SimpMessageHeaderAccessor accessor = mock(SimpMessageHeaderAccessor.class);
    given(accessor.getSessionAttributes()).willReturn(null);

    // when & then
    assertThatThrownBy(() -> controller.sendDirectMessage(request, accessor))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(UserErrorCode.JWT_TOKEN_INVALID);
  }

  @Test
  @DisplayName("세션에 USER_ID_KEY가 없으면 JWT_TOKEN_INVALID 예외가 발생한다.")
  void sendDirectMessage_userIdKeyMissing_throwsBusinessException() {

    // given
    UUID senderId = UUID.randomUUID();
    DirectMessageCreateRequest request =
        new DirectMessageCreateRequest(senderId, UUID.randomUUID(), "테스트 메시지");

    SimpMessageHeaderAccessor accessor = mock(SimpMessageHeaderAccessor.class);
    given(accessor.getSessionAttributes()).willReturn(new HashMap<>());

    // when & then
    assertThatThrownBy(() -> controller.sendDirectMessage(request, accessor))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(UserErrorCode.JWT_TOKEN_INVALID);
  }

  @Test
  @DisplayName("세션 userId와 senderId가 다르면 서비스가 UNAUTHORIZED_MESSAGE_ACCESS 예외를 발생시키고 컨트롤러가 전파한다.")
  void sendDirectMessage_senderMismatch_propagatesUnauthorizedException() {

    // given
    UUID authUserId = UUID.randomUUID();
    UUID differentSenderId = UUID.randomUUID();
    DirectMessageCreateRequest request =
        new DirectMessageCreateRequest(differentSenderId, UUID.randomUUID(), "테스트 메시지");

    SimpMessageHeaderAccessor accessor = mock(SimpMessageHeaderAccessor.class);
    Map<String, Object> sessionAttrs = new HashMap<>();
    sessionAttrs.put(StompAuthChannelInterceptor.USER_ID_KEY, authUserId);
    given(accessor.getSessionAttributes()).willReturn(sessionAttrs);

    willThrow(new BusinessException(MessageErrorCode.UNAUTHORIZED_MESSAGE_ACCESS))
        .given(directMessageService).send(request, authUserId);

    // when & then
    assertThatThrownBy(() -> controller.sendDirectMessage(request, accessor))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(MessageErrorCode.UNAUTHORIZED_MESSAGE_ACCESS);
  }

  @Test
  @DisplayName("서비스에서 BusinessException 발생 시 컨트롤러가 예외를 전파한다.")
  void sendDirectMessage_serviceThrowsBusinessException_propagates() {

    // given
    UUID authUserId = UUID.randomUUID();
    DirectMessageCreateRequest request =
        new DirectMessageCreateRequest(authUserId, UUID.randomUUID(), "테스트 메시지");

    SimpMessageHeaderAccessor accessor = mock(SimpMessageHeaderAccessor.class);
    Map<String, Object> sessionAttrs = new HashMap<>();
    sessionAttrs.put(StompAuthChannelInterceptor.USER_ID_KEY, authUserId);
    given(accessor.getSessionAttributes()).willReturn(sessionAttrs);

    willThrow(new BusinessException(MessageErrorCode.MESSAGE_NOT_FOUND))
        .given(directMessageService).send(request, authUserId);

    // when & then
    assertThatThrownBy(() -> controller.sendDirectMessage(request, accessor))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(MessageErrorCode.MESSAGE_NOT_FOUND);
  }

}
