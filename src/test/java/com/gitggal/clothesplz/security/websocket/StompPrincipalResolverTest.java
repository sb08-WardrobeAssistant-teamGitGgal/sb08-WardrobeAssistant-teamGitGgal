package com.gitggal.clothesplz.security.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

@DisplayName("StompPrincipalResolver 테스트")
class StompPrincipalResolverTest {

  @Test
  @DisplayName("세션 속성이 null이면 예외를 던진다.")
  void getUserId_nullAttrs_throws() {
    SimpMessageHeaderAccessor accessor = mock(SimpMessageHeaderAccessor.class);
    given(accessor.getSessionAttributes()).willReturn(null);

    assertThatThrownBy(() -> StompPrincipalResolver.getUserId(accessor))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex ->
            assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(UserErrorCode.JWT_TOKEN_INVALID));
  }

  @Test
  @DisplayName("세션 속성에 userId 키가 없으면 예외를 던진다.")
  void getUserId_missingKey_throws() {
    SimpMessageHeaderAccessor accessor = mock(SimpMessageHeaderAccessor.class);
    given(accessor.getSessionAttributes()).willReturn(new HashMap<>());

    assertThatThrownBy(() -> StompPrincipalResolver.getUserId(accessor))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex ->
            assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(UserErrorCode.JWT_TOKEN_INVALID));
  }

  @Test
  @DisplayName("userId 값이 UUID 타입이 아니면 예외를 던진다.")
  void getUserId_nonUuidValue_throws() {
    SimpMessageHeaderAccessor accessor = mock(SimpMessageHeaderAccessor.class);
    Map<String, Object> attrs = new HashMap<>();
    attrs.put(StompAuthChannelInterceptor.USER_ID_KEY, "not-a-uuid");
    given(accessor.getSessionAttributes()).willReturn(attrs);

    assertThatThrownBy(() -> StompPrincipalResolver.getUserId(accessor))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex ->
            assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(UserErrorCode.JWT_TOKEN_INVALID));
  }

  @Test
  @DisplayName("세션에 UUID가 있으면 반환한다.")
  void getUserId_validUuid_returns() {
    UUID userId = UUID.randomUUID();
    SimpMessageHeaderAccessor accessor = mock(SimpMessageHeaderAccessor.class);
    Map<String, Object> attrs = new HashMap<>();
    attrs.put(StompAuthChannelInterceptor.USER_ID_KEY, userId);
    given(accessor.getSessionAttributes()).willReturn(attrs);

    UUID result = StompPrincipalResolver.getUserId(accessor);

    assertThat(result).isEqualTo(userId);
  }
}
