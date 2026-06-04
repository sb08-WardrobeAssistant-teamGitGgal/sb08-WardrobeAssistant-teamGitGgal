package com.gitggal.clothesplz.security.oauth;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisOAuthAuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

  private static final String PREFIX = "auth:oauth2:request:";
  private static final Duration TTL = Duration.ofMinutes(3);

  private final RedisTemplate<String, Object> redisTemplate;

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    String state = request.getParameter(OAuth2ParameterNames.STATE);
    if (!StringUtils.hasText(state)) {
      return null;
    }

    Object raw = redisTemplate.opsForValue().get(PREFIX + state);
    if (raw instanceof String serialized) {
      return deserialize(serialized);
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

    redisTemplate.opsForValue().set(PREFIX + authorizationRequest.getState(),
        serialize(authorizationRequest), TTL);
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
    try (
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)
    ) {
      objectOutputStream.writeObject(authorizationRequest);
      objectOutputStream.flush();
      return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
    } catch (Exception e) {
      log.warn("[OAuth] Authorization request 직렬화 실패: message = {}", e.getMessage());
      throw new BusinessException(UserErrorCode.OAUTH_AUTHORIZATION_REQUEST_SERIALIZATION_FAILED);
    }
  }

  private OAuth2AuthorizationRequest deserialize(String serialized) {
    try (
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(Base64.getDecoder().decode(serialized));
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)
    ) {
      Object object = objectInputStream.readObject();
      if (object instanceof OAuth2AuthorizationRequest authorizationRequest) {
        return authorizationRequest;
      }
    } catch (Exception e) {
      log.warn("[OAuth] Authorization request 역직렬화 실패: message = {}", e.getMessage());
    }
    return null;
  }
}
