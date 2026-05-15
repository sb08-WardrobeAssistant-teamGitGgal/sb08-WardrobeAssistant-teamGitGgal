package com.gitggal.clothesplz.security.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.security.jwt.JwtRegistry;
import com.gitggal.clothesplz.security.jwt.JwtTokenProvider;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

@ExtendWith(MockitoExtension.class)
@DisplayName("StompAuthChannelInterceptor 테스트")
class StompAuthChannelInterceptorTest {

  @Mock
  private JwtTokenProvider tokenProvider;

  @Mock
  private JwtRegistry jwtRegistry;

  @Mock
  private MessageChannel channel;

  @InjectMocks
  private StompAuthChannelInterceptor interceptor;

  private Message<byte[]> stompMessage(StompCommand command, String authHeader) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
    accessor.setSessionAttributes(new HashMap<>());
    if (authHeader != null) {
      accessor.setNativeHeader("Authorization", authHeader);
    }
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  @Test
  @DisplayName("accessor가 null이면 메시지를 그대로 반환한다.")
  void preSend_nullAccessor_passesThrough() {
    Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();
    Message<?> result = interceptor.preSend(message, channel);
    assertThat(result).isSameAs(message);
  }

  @Test
  @DisplayName("CONNECT가 아닌 명령은 인증 검사 없이 통과한다.")
  void preSend_nonConnectCommand_passesThrough() {
    Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, null);
    Message<?> result = interceptor.preSend(message, channel);
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("CONNECT에 Authorization 헤더가 없으면 예외를 던진다.")
  void preSend_connect_noHeader_throws() {
    Message<byte[]> message = stompMessage(StompCommand.CONNECT, null);

    assertThatThrownBy(() -> interceptor.preSend(message, channel))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex ->
            assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(UserErrorCode.JWT_TOKEN_INVALID));
  }

  @Test
  @DisplayName("CONNECT에 Bearer 형식이 아닌 헤더면 예외를 던진다.")
  void preSend_connect_nonBearerHeader_throws() {
    Message<byte[]> message = stompMessage(StompCommand.CONNECT, "NotBearer token");

    assertThatThrownBy(() -> interceptor.preSend(message, channel))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex ->
            assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(UserErrorCode.JWT_TOKEN_INVALID));
  }

  @Test
  @DisplayName("토큰 검증 실패 시 예외를 던진다.")
  void preSend_connect_invalidToken_throws() {
    given(tokenProvider.validateAccessToken("bad-token")).willReturn(false);

    Message<byte[]> message = stompMessage(StompCommand.CONNECT, "Bearer bad-token");

    assertThatThrownBy(() -> interceptor.preSend(message, channel))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex ->
            assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(UserErrorCode.JWT_TOKEN_INVALID));
  }

  @Test
  @DisplayName("Registry에 없는 토큰이면 예외를 던진다.")
  void preSend_connect_inactiveToken_throws() {
    given(tokenProvider.validateAccessToken("valid-token")).willReturn(true);
    given(jwtRegistry.hasActiveJwtInformationByAccessToken("valid-token")).willReturn(false);

    Message<byte[]> message = stompMessage(StompCommand.CONNECT, "Bearer valid-token");

    assertThatThrownBy(() -> interceptor.preSend(message, channel))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex ->
            assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(UserErrorCode.JWT_TOKEN_INVALID));
  }

  @Test
  @DisplayName("유효한 토큰이면 세션에 userId를 저장하고 메시지를 반환한다.")
  void preSend_connect_validToken_storesUserIdAndReturns() {
    UUID userId = UUID.randomUUID();
    given(tokenProvider.validateAccessToken("valid-token")).willReturn(true);
    given(jwtRegistry.hasActiveJwtInformationByAccessToken("valid-token")).willReturn(true);
    given(tokenProvider.getUserId("valid-token")).willReturn(userId);

    Message<byte[]> message = stompMessage(StompCommand.CONNECT, "Bearer valid-token");
    Message<?> result = interceptor.preSend(message, channel);

    assertThat(result).isNotNull();
    StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
    assertThat(resultAccessor.getSessionAttributes())
        .containsEntry(StompAuthChannelInterceptor.USER_ID_KEY, userId);
  }
}
