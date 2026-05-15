package com.gitggal.clothesplz.security.websocket;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import java.util.Map;
import java.util.UUID;
import lombok.NoArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

/**
 * STOMP 메시지 헤더에서 인증이 완료된 사용자 ID를 꺼내는 헬퍼 클래스
 */
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class StompPrincipalResolver {

  public static UUID getUserId(SimpMessageHeaderAccessor accessor) {

    Map<String, Object> attrs = accessor.getSessionAttributes();

    if (attrs == null) throw new BusinessException(UserErrorCode.JWT_TOKEN_INVALID);

    Object v = attrs.get(StompAuthChannelInterceptor.USER_ID_KEY);

    if (!(v instanceof UUID userId)) {
      throw new BusinessException(UserErrorCode.JWT_TOKEN_INVALID);
    }

    return userId;
  }
}
