package com.gitggal.clothesplz.security.websocket;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.security.jwt.JwtRegistry;
import com.gitggal.clothesplz.security.jwt.JwtTokenProvider;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * STOMP CONNECT 프레임 - JWT 인증 인터셉터
 *
 * CONNECT 프레임이 들어올 때 Authorization 헤더에서 JWT를 꺼내 검증
 *
 * - 검증 성공 시 세션 속성에 userId(UUID)를 저장 → 이후 메시지에서 활용
 * - 검증 실패 시 BusinessException으로 연결 거부
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

  public static final String USER_ID_KEY = "userId";

  private final JwtTokenProvider tokenProvider;
  private final JwtRegistry jwtRegistry;

  /**
   * 메시지가 채널로 전달되기 직전에 호출되는 메서드
   */
  @Override
  public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {

    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor == null) return message;

    // CONNECT 시점에만 인증 검증
    if (StompCommand.CONNECT.equals(accessor.getCommand())) {

      String token = resolveToken(accessor);

      if (!StringUtils.hasText(token)) {
        log.warn("[STOMP] CONNECT 실패: Authorization 헤더 없음");
        throw new BusinessException(UserErrorCode.JWT_TOKEN_INVALID);
      }
      if (!tokenProvider.validateAccessToken(token)) {
        log.warn("[STOMP] CONNECT 실패: 토큰 검증 실패");
        throw new BusinessException(UserErrorCode.JWT_TOKEN_INVALID);
      }
      if (!jwtRegistry.hasActiveJwtInformationByAccessToken(token)) {
        log.warn("[STOMP] CONNECT 실패: 활성 토큰이 아님");
        throw new BusinessException(UserErrorCode.JWT_TOKEN_INVALID);
      }

      UUID userId = tokenProvider.getUserId(token);

      accessor.getSessionAttributes().put(USER_ID_KEY, userId);

      log.info("[STOMP] CONNECT 성공: userId={}", userId);
    }

    return message;
  }

  /**
   * Authorization 헤더에서 토큰값 추출
   */
  private String resolveToken(StompHeaderAccessor accessor) {

    List<String> values = accessor.getNativeHeader("Authorization");

    if (values == null || values.isEmpty()) return null;

    String bearer = values.get(0);

    if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
      return bearer.substring(7);
    }

    return null;
  }
}
