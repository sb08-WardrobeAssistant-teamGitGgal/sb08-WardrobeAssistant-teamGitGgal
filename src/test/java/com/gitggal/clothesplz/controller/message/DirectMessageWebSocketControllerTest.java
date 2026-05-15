package com.gitggal.clothesplz.controller.message;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.gitggal.clothesplz.dto.message.DirectMessageCreateRequest;
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
}
