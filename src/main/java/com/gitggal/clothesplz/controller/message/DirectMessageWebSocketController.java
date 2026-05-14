package com.gitggal.clothesplz.controller.message;

import com.gitggal.clothesplz.dto.message.DirectMessageCreateRequest;
import com.gitggal.clothesplz.security.websocket.StompPrincipalResolver;
import com.gitggal.clothesplz.service.message.DirectMessageService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

/**
 * DM 컨트롤러
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DirectMessageWebSocketController {

  private final DirectMessageService directMessageService;

  @MessageMapping("/direct-messages_send")
  public void sendDirectMessage(
      @Payload @Valid DirectMessageCreateRequest request,
      SimpMessageHeaderAccessor accessor) {

    UUID authUserId = StompPrincipalResolver.getUserId(accessor);

    log.info("[WS] DM 송신: authUserId={}, request={}", authUserId, request);

    directMessageService.send(request, authUserId);
  }
}
