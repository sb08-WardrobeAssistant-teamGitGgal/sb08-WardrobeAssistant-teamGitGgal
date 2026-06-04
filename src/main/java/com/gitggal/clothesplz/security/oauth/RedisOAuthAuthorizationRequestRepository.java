package com.gitggal.clothesplz.security.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisOAuthAuthorizationRequestRepository implements
    AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

  private static final String PREFIX = "auth:oauth2:request:";
  private static final Duration TTL = Duration.ofMinutes(10);

  private final RedisTemplate<String, Object> redisTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper()
      .registerModules(SecurityJackson2Modules.getModules(
          RedisOAuthAuthorizationRequestRepository.class.getClassLoader()))
      .registerModule(new OAuth2ClientJackson2Module());

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    String state = request.getParameter(OAuth2ParameterNames.STATE);
    if (!StringUtils.hasText(state)) {
      return null;
    }

    Object raw = redisTemplate.opsForValue().get(PREFIX + state);
    if (raw instanceof String json) {
      return deserialize(json);
    }
    return null;
  }

  @Override
  public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
      HttpServletRequest request, HttpServletResponse response) {
    if (authorizationRequest == null) {
      String state = request.getParameter(OAuth2ParameterNames.STATE);
      if (StringUtils.hasText(state)) {
        redisTemplate.delete(PREFIX + state);
      }
      return;
    }
    if (!StringUtils.hasText(authorizationRequest.getState())) {
      log.warn("[OAuth] Authorization request 저장 실패: state가 비어있습니다.");
      return;
    }
    redisTemplate.opsForValue().set(
        PREFIX + authorizationRequest.getState(),
        serialize(authorizationRequest),
        TTL
    );
  }

  @Override
  public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
      HttpServletResponse response) {
    OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
    String state = request.getParameter(OAuth2ParameterNames.STATE);
    if (StringUtils.hasText(state)) {
      redisTemplate.delete(PREFIX + state);
    }
    return authRequest;
  }

  private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
    try {
      return objectMapper.writeValueAsString(authorizationRequest);
    } catch (Exception e) {
      log.warn("[OAuth] Authorization request 직렬화 실패: message = {}", e.getMessage(), e);
      throw new BusinessException(UserErrorCode.OAUTH_AUTHORIZATION_REQUEST_SERIALIZATION_FAILED);
    }
  }

  private OAuth2AuthorizationRequest deserialize(String json) {
    try {
      return objectMapper.readValue(json, OAuth2AuthorizationRequest.class);
    } catch (Exception e) {
      log.warn("[OAuth] Authorization request 역직렬화 실패: message = {}", e.getMessage(), e);
      throw new BusinessException(UserErrorCode.OAUTH_AUTHORIZATION_REQUEST_SERIALIZATION_FAILED);
    }
  }
}
