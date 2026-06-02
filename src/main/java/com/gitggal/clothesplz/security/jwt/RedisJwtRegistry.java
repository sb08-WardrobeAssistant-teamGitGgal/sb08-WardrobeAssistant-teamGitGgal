package com.gitggal.clothesplz.security.jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class RedisJwtRegistry implements JwtRegistry {

  private static final String USER_KEY_PREFIX = "auth:jwt:user:";
  private static final String ACCESS_INDEX_PREFIX = "auth:jwt:access:";
  private static final String REFRESH_INDEX_PREFIX = "auth:jwt:refresh:";
  private static final String REFRESH_LOCK_PREFIX = "auth:jwt:refresh-lock:";
  private static final String ACCESS_TOKEN_HASH_KEY = "accessToken";
  private static final String REFRESH_TOKEN_HASH_KEY = "refreshToken";
  private static final Duration ROTATE_LOCK_TTL = Duration.ofSeconds(10);

  private final RedisTemplate<String, Object> redisTemplate;
  private final JwtTokenProvider tokenProvider;

  @Override
  public void registerJwtInformation(JwtInformation jwtInformation) {
    UUID userId = jwtInformation.userDto().id();

    removeExistingTokenIndexes(userId);
    saveJwtInformation(userId, jwtInformation);
  }

  @Override
  public void invalidateJwtInformationByUserId(UUID userId) {
    removeExistingTokenIndexes(userId);
    redisTemplate.delete(userKey(userId));
  }

  @Override
  public boolean hasActiveJwtInformationByUserId(UUID userId) {
    JwtTokens tokens = findTokensByUserId(userId);

    if (tokens == null) {
      return false;
    }

    boolean isValid = tokenProvider.validateAccessToken(tokens.accessToken())
        && tokenProvider.validateRefreshToken(tokens.refreshToken());

    if (!isValid) {
      invalidateJwtInformationByUserId(userId);
      return false;
    }

    return true;
  }

  @Override
  public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
    String userId = findUserIdByAccessToken(accessToken);

    if (userId == null) {
      return false;
    }

    JwtTokens tokens = findTokensByUserId(UUID.fromString(userId));
    if (tokens == null || !accessToken.equals(tokens.accessToken())) {
      invalidateJwtInformationByUserId(UUID.fromString(userId));
      return false;
    }

    if (!tokenProvider.validateAccessToken(accessToken)) {
      invalidateJwtInformationByUserId(UUID.fromString(userId));
      return false;
    }

    return true;
  }

  @Override
  public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
    String userId = findUserIdByRefreshToken(refreshToken);

    if (userId == null) {
      return false;
    }

    JwtTokens tokens = findTokensByUserId(UUID.fromString(userId));
    if (tokens == null || !refreshToken.equals(tokens.refreshToken())) {
      invalidateJwtInformationByUserId(UUID.fromString(userId));
      return false;
    }

    if (!tokenProvider.validateRefreshToken(refreshToken)) {
      invalidateJwtInformationByUserId(UUID.fromString(userId));
      return false;
    }

    return true;
  }

  @Override
  public boolean rotateJwtInformation(String oldRefreshToken, JwtInformation newJwtInformation) {
    String lockKey = refreshLockKey(oldRefreshToken);
    Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", ROTATE_LOCK_TTL);

    if (!Boolean.TRUE.equals(locked)) {
      return false;
    }

    try {
      String userIdValue = findUserIdByRefreshToken(oldRefreshToken);
      UUID userId = newJwtInformation.userDto().id();

      if (userIdValue == null || !userId.toString().equals(userIdValue)) {
        return false;
      }

      JwtTokens currentTokens = findTokensByUserId(userId);
      if (currentTokens == null || !oldRefreshToken.equals(currentTokens.refreshToken())) {
        return false;
      }

      removeTokenIndexes(currentTokens);
      saveJwtInformation(userId, newJwtInformation);
      return true;
    } finally {
      redisTemplate.delete(lockKey);
    }
  }

  @Override
  public void clearExpiredJwtInformation() {
    // Redis TTL에 의해 만료된 로그인 정보는 자동으로 삭제
  }

  private void saveJwtInformation(UUID userId, JwtInformation jwtInformation) {
    Duration accessTtl = ttlUntil(jwtInformation.accessTokenExpiry());
    Duration refreshTtl = ttlUntil(jwtInformation.refreshTokenExpiry());

    if (accessTtl.isZero() || refreshTtl.isZero()) {
      return;
    }

    String userIdValue = userId.toString();

    redisTemplate.opsForHash().put(userKey(userId), ACCESS_TOKEN_HASH_KEY,
        jwtInformation.accessToken()); // 사용자 ID -> 토큰 조회
    redisTemplate.opsForHash().put(userKey(userId), REFRESH_TOKEN_HASH_KEY,
        jwtInformation.refreshToken());
    redisTemplate.expire(userKey(userId), refreshTtl);

    redisTemplate.opsForValue().set(accessKey(jwtInformation.accessToken()), userIdValue,
        accessTtl); // 토큰 -> 사용자 ID 조회
    redisTemplate.opsForValue().set(refreshKey(jwtInformation.refreshToken()), userIdValue,
        refreshTtl);
  }

  private void removeExistingTokenIndexes(UUID userId) {
    JwtTokens tokens = findTokensByUserId(userId);
    if (tokens != null) {
      removeTokenIndexes(tokens);
    }
  }

  private void removeTokenIndexes(JwtTokens tokens) {
    redisTemplate.delete(accessKey(tokens.accessToken()));
    redisTemplate.delete(refreshKey(tokens.refreshToken()));
  }

  private JwtTokens findTokensByUserId(UUID userId) {
    Map<Object, Object> values = redisTemplate.opsForHash().entries(userKey(userId));

    if (values.isEmpty()) {
      return null;
    }

    String accessToken = asString(values.get(ACCESS_TOKEN_HASH_KEY));
    String refreshToken = asString(values.get(REFRESH_TOKEN_HASH_KEY));

    if (accessToken == null || refreshToken == null) {
      return null;
    }

    return new JwtTokens(accessToken, refreshToken);
  }

  private String findUserIdByAccessToken(String accessToken) {
    return asString(redisTemplate.opsForValue().get(accessKey(accessToken)));
  }

  private String findUserIdByRefreshToken(String refreshToken) {
    return asString(redisTemplate.opsForValue().get(refreshKey(refreshToken)));
  }

  private Duration ttlUntil(Instant expiry) {
    Duration ttl = Duration.between(Instant.now(), expiry);
    return ttl.isNegative() ? Duration.ZERO : ttl;
  }

  private String asString(Object value) {
    return value instanceof String string ? string : null;
  }

  private String userKey(UUID userId) {
    return USER_KEY_PREFIX + userId;
  }

  private String accessKey(String accessToken) {
    String hash = DigestUtils.sha256Hex(accessToken);
    return ACCESS_INDEX_PREFIX + hash;
  }

  private String refreshKey(String refreshToken) {
    String hash = DigestUtils.sha256Hex(refreshToken);
    return REFRESH_INDEX_PREFIX + hash;
  }

  private String refreshLockKey(String refreshToken) {
    String hash = DigestUtils.sha256Hex(refreshToken);
    return REFRESH_LOCK_PREFIX + hash;
  }

  private record JwtTokens(String accessToken, String refreshToken) {

  }
}
