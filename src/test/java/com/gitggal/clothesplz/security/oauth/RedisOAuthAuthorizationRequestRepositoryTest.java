package com.gitggal.clothesplz.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

  private RedisOAuthAuthorizationRequestRepository repository;

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private OAuth2AuthorizationRequest authorizationRequest;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();

    repository = new RedisOAuthAuthorizationRequestRepository(
        redisTemplate,
        objectMapper
    );

    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
        .clientId("test-client")
        .authorizationUri("https://accounts.google.com/o/oauth2/auth")
        .redirectUri("https://example.com/login/oauth2/code/google")
        .scopes(new HashSet<>(Set.of("openid", "email", "profile")))
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
    @DisplayName("Redis에 저장된 JSON 문자열을 역직렬화하여 반환")
    void returnsAuthorizationRequestWhenFoundInRedis() {
      given(redisTemplate.opsForValue()).willReturn(valueOperations);

      repository.saveAuthorizationRequest(authorizationRequest, request, response);
      ArgumentCaptor<String> serializedCaptor = ArgumentCaptor.forClass(String.class);
      verify(valueOperations).set(
          eq(PREFIX + STATE),
          serializedCaptor.capture(),
          eq(Duration.ofMinutes(10))
      );

      String capturedJson = serializedCaptor.getValue();
      assertThat(capturedJson).startsWith("{");
      assertThat(capturedJson).contains("\"state\"");
      assertThat(capturedJson).contains(STATE);

      request.setParameter("state", STATE);
      given(valueOperations.get(PREFIX + STATE)).willReturn(capturedJson);
      OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);

      assertThat(result).isNotNull();
      assertThat(result.getState()).isEqualTo(STATE);
      assertThat(result.getClientId()).isEqualTo("test-client");
      assertThat(result.getGrantType()).isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
    }

    @Test
    @DisplayName("Redis 값이 String 타입이 아니면 null을 반환")
    void returnsNullWhenRedisValueIsNotString() {
      request.setParameter("state", STATE);
      given(redisTemplate.opsForValue()).willReturn(valueOperations);
      given(valueOperations.get(PREFIX + STATE)).willReturn(12345);

      OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);

      assertThat(result).isNull();
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
    @DisplayName("authorizationRequest를 JSON 직렬화하여 Redis에 TTL과 함께 저장")
    void savesSerializedJsonToRedisWithTtl() {
      given(redisTemplate.opsForValue()).willReturn(valueOperations);

      repository.saveAuthorizationRequest(authorizationRequest, request, response);

      ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
      verify(valueOperations).set(
          eq(PREFIX + STATE),
          jsonCaptor.capture(),
          eq(Duration.ofMinutes(10))
      );

      String savedValue = jsonCaptor.getValue();
      assertThat(savedValue).startsWith("{");
      assertThat(savedValue).contains("\"state\"");
      assertThat(savedValue).contains(STATE);
    }

    @Test
    @DisplayName("state가 비어있는 authorizationRequest는 저장하지 않음")
    void doesNotSaveWhenStateIsEmpty() {
      OAuth2AuthorizationRequest noStateRequest = OAuth2AuthorizationRequest.authorizationCode()
          .clientId("test-client")
          .authorizationUri("https://accounts.google.com/o/oauth2/auth")
          .redirectUri("https://example.com/login/oauth2/code/google")
          .scopes(new HashSet<>(Set.of("openid")))
          .state("")
          .build();

      repository.saveAuthorizationRequest(noStateRequest, request, response);

      verify(redisTemplate, never()).opsForValue();
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
      verify(valueOperations).set(
          eq(PREFIX + STATE),
          serializedCaptor.capture(),
          eq(Duration.ofMinutes(10))
      );

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
    @DisplayName("Redis에 값이 없어도 state가 있으면 삭제를 시도하고 null을 반환")
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