package com.gitggal.clothesplz.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

@ExtendWith(MockitoExtension.class)
class RedisOAuthAuthorizationRequestRepositoryTest {

  private static final String PREFIX = "auth:oauth2:request:";
  private static final String STATE = "test-state-value";

  @Mock
  private RedisTemplate<String, Object> redisTemplate;

  @Mock
  private ValueOperations<String, Object> valueOperations;

  @InjectMocks
  private RedisOAuthAuthorizationRequestRepository repository;

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private OAuth2AuthorizationRequest authorizationRequest;

  @BeforeEach
  void setUp() {
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
        .clientId("test-client")
        .authorizationUri("https://accounts.google.com/o/oauth2/auth")
        .redirectUri("https://example.com/login/oauth2/code/google")
        .scopes(Set.of("openid", "email", "profile"))
        .state(STATE)
        .additionalParameters(Map.of("nonce", "test-nonce"))
        .build();
  }

  @Nested
  @DisplayName("loadAuthorizationRequest")
  class LoadAuthorizationRequest {

    @Test
    @DisplayName("state 파라미터가 없으면 null을 반환")
    void returnsNullWhenStateIsMissing() {
      OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);

      assertThat(result).isNull();
    }

    @Test
    @DisplayName("Redis에 저장된 값이 없으면 null을 반환")
    void returnsNullWhenNotFoundInRedis() {
      request.setParameter("state", STATE);
      given(redisTemplate.opsForValue()).willReturn(valueOperations);
      given(valueOperations.get(PREFIX + STATE)).willReturn(null);

      OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);

      assertThat(result).isNull();
    }

    @Test
    @DisplayName("Redis에 저장된 값이 있으면 OAuth2AuthorizationRequest를 반환")
    void returnsAuthorizationRequestWhenFoundInRedis() {
      given(redisTemplate.opsForValue()).willReturn(valueOperations);
      repository.saveAuthorizationRequest(authorizationRequest, request, response);
      ArgumentCaptor<String> serializedCaptor = ArgumentCaptor.forClass(String.class);
      verify(valueOperations).set(eq(PREFIX + STATE), serializedCaptor.capture(),
          eq(Duration.ofMinutes(3)));

      request.setParameter("state", STATE);
      given(valueOperations.get(PREFIX + STATE)).willReturn(serializedCaptor.getValue());
      OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);

      assertThat(result).isNotNull();
      assertThat(result.getState()).isEqualTo(STATE);
      assertThat(result.getClientId()).isEqualTo("test-client");
      assertThat(result.getGrantType()).isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
    }
  }

  @Nested
  @DisplayName("saveAuthorizationRequest")
  class SaveAuthorizationRequest {

    @Test
    @DisplayName("authorizationRequest가 null이고 state가 있으면 Redis에서 삭제")
    void deletesFromRedisWhenAuthorizationRequestIsNullWithState() {
      request.setParameter("state", STATE);

      repository.saveAuthorizationRequest(null, request, response);

      verify(redisTemplate).delete(PREFIX + STATE);
    }

    @Test
    @DisplayName("authorizationRequest가 null이고 state도 없으면 삭제 X")
    void doesNotDeleteWhenAuthorizationRequestIsNullWithoutState() {
      repository.saveAuthorizationRequest(null, request, response);

      verify(redisTemplate, never()).delete(any(String.class));
    }

    @Test
    @DisplayName("authorizationRequest를 Redis에 TTL과 함께 저장")
    void savesAuthorizationRequestToRedisWithTtl() {
      given(redisTemplate.opsForValue()).willReturn(valueOperations);

      repository.saveAuthorizationRequest(authorizationRequest, request, response);

      verify(valueOperations).set(
          eq(PREFIX + STATE),
          any(String.class),
          eq(Duration.ofMinutes(3))
      );
    }
  }

  @Nested
  @DisplayName("removeAuthorizationRequest")
  class RemoveAuthorizationRequest {

    @Test
    @DisplayName("state가 있으면 Redis에서 삭제하고 기존 값을 반환")
    void deletesAndReturnsAuthorizationRequest() {
      given(redisTemplate.opsForValue()).willReturn(valueOperations);
      repository.saveAuthorizationRequest(authorizationRequest, request, response);
      ArgumentCaptor<String> serializedCaptor = ArgumentCaptor.forClass(String.class);
      verify(valueOperations).set(eq(PREFIX + STATE), serializedCaptor.capture(),
          eq(Duration.ofMinutes(3)));

      request.setParameter("state", STATE);
      given(valueOperations.get(PREFIX + STATE)).willReturn(serializedCaptor.getValue());

      OAuth2AuthorizationRequest result = repository.removeAuthorizationRequest(request, response);

      assertThat(result).isNotNull();
      assertThat(result.getState()).isEqualTo(STATE);
      verify(redisTemplate).delete(PREFIX + STATE);
    }

    @Test
    @DisplayName("state가 없으면 삭제하지 않고 null을 반환")
    void returnsNullWhenStateIsMissing() {
      OAuth2AuthorizationRequest result = repository.removeAuthorizationRequest(request, response);

      assertThat(result).isNull();
      verify(redisTemplate, never()).delete(any(String.class));
    }

    @Test
    @DisplayName("Redis에 값이 없어도 삭제를 시도하고 null을 반환")
    void deletesEvenWhenNotFoundInRedis() {
      request.setParameter("state", STATE);
      given(redisTemplate.opsForValue()).willReturn(valueOperations);
      given(valueOperations.get(PREFIX + STATE)).willReturn(null);

      OAuth2AuthorizationRequest result = repository.removeAuthorizationRequest(request, response);

      assertThat(result).isNull();
      verify(redisTemplate).delete(PREFIX + STATE);
    }
  }
}
