package com.gitggal.clothesplz.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.user.UserRole;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisJwtRegistryTest {

  @Mock
  private RedisTemplate<String, Object> redisTemplate;

  @Mock
  private ValueOperations<String, Object> valueOperations;

  @Mock
  private HashOperations<String, Object, Object> hashOperations;

  @Mock
  private JwtTokenProvider tokenProvider;

  private RedisJwtRegistry jwtRegistry;
  private UserDto userDto;

  @BeforeEach
  void setUp() {
    jwtRegistry = new RedisJwtRegistry(redisTemplate, tokenProvider);
    userDto = new UserDto(
        UUID.randomUUID(),
        Instant.now(),
        "test@test.com",
        "홍길동",
        UserRole.USER,
        false
    );
  }

  @Test
  @DisplayName("JWT 등록")
  void registerJwtInformation() {
    JwtInformation jwtInformation = jwtInformation("access-token", "refresh-token");
    givenRedisHashOperations();
    givenRedisValueOperations();
    given(hashOperations.entries(userKey())).willReturn(Map.of());

    jwtRegistry.registerJwtInformation(jwtInformation);

    verify(hashOperations).put(userKey(), "accessToken", "access-token");
    verify(hashOperations).put(userKey(), "refreshToken", "refresh-token");
    verify(redisTemplate).expire(eq(userKey()), any(Duration.class));
    verify(valueOperations).set(eq(accessKey("access-token")), eq(userDto.id().toString()),
        any(Duration.class));
    verify(valueOperations).set(eq(refreshKey("refresh-token")), eq(userDto.id().toString()),
        any(Duration.class));
  }

  @Test
  @DisplayName("Access Token 조회 - 활성")
  void hasActiveJwtInformationByAccessToken() {
    givenRedisHashOperations();
    givenRedisValueOperations();
    given(valueOperations.get(accessKey("access-token"))).willReturn(userDto.id().toString());
    given(hashOperations.entries(userKey()))
        .willReturn(Map.of("accessToken", "access-token", "refreshToken", "refresh-token"));
    given(tokenProvider.validateAccessToken("access-token")).willReturn(true);

    boolean active = jwtRegistry.hasActiveJwtInformationByAccessToken("access-token");

    assertThat(active).isTrue();
  }

  @Test
  @DisplayName("Access Token 조회 - 무효화")
  void hasActiveJwtInformationByAccessToken_mismatch() {
    givenRedisHashOperations();
    givenRedisValueOperations();
    given(valueOperations.get(accessKey("access-token"))).willReturn(userDto.id().toString());
    given(hashOperations.entries(userKey()))
        .willReturn(Map.of("accessToken", "other-access-token", "refreshToken", "refresh-token"));

    boolean active = jwtRegistry.hasActiveJwtInformationByAccessToken("access-token");

    assertThat(active).isFalse();
    verify(redisTemplate).delete(accessKey("other-access-token"));
    verify(redisTemplate).delete(refreshKey("refresh-token"));
    verify(redisTemplate).delete(userKey());
  }

  @Test
  @DisplayName("Refresh Token 조회 - 활성")
  void hasActiveJwtInformationByRefreshToken() {
    givenRedisHashOperations();
    givenRedisValueOperations();
    given(valueOperations.get(refreshKey("refresh-token"))).willReturn(userDto.id().toString());
    given(hashOperations.entries(userKey()))
        .willReturn(Map.of("accessToken", "access-token", "refreshToken", "refresh-token"));
    given(tokenProvider.validateRefreshToken("refresh-token")).willReturn(true);

    boolean active = jwtRegistry.hasActiveJwtInformationByRefreshToken("refresh-token");

    assertThat(active).isTrue();
  }

  @Test
  @DisplayName("Refresh Token 조회 - 무효화")
  void hasActiveJwtInformationByRefreshToken_mismatch() {
    givenRedisHashOperations();
    givenRedisValueOperations();
    given(valueOperations.get(refreshKey("refresh-token"))).willReturn(userDto.id().toString());
    given(hashOperations.entries(userKey()))
        .willReturn(Map.of("accessToken", "access-token", "refreshToken", "other-refresh-token"));

    boolean active = jwtRegistry.hasActiveJwtInformationByRefreshToken("refresh-token");

    assertThat(active).isFalse();
    verify(redisTemplate).delete(accessKey("access-token"));
    verify(redisTemplate).delete(refreshKey("other-refresh-token"));
    verify(redisTemplate).delete(userKey());
  }

  @Test
  @DisplayName("JWT 무효화")
  void invalidateJwtInformationByUserId() {
    givenRedisHashOperations();
    given(hashOperations.entries(userKey()))
        .willReturn(Map.of("accessToken", "access-token", "refreshToken", "refresh-token"));

    jwtRegistry.invalidateJwtInformationByUserId(userDto.id());

    verify(redisTemplate).delete(accessKey("access-token"));
    verify(redisTemplate).delete(refreshKey("refresh-token"));
    verify(redisTemplate).delete(userKey());
  }

  @Test
  @DisplayName("JWT 교체 - 성공")
  void rotateJwtInformation() {
    JwtInformation newJwtInformation = jwtInformation("new-access-token", "new-refresh-token");

    givenRedisHashOperations();
    givenRedisValueOperations();
    given(valueOperations.setIfAbsent(eq(refreshLockKey("old-refresh-token")), eq("1"),
        any(Duration.class))).willReturn(true);
    given(valueOperations.get(refreshKey("old-refresh-token"))).willReturn(userDto.id().toString());
    given(hashOperations.entries(userKey()))
        .willReturn(Map.of("accessToken", "old-access-token", "refreshToken",
            "old-refresh-token"));

    boolean rotated = jwtRegistry.rotateJwtInformation("old-refresh-token", newJwtInformation);

    assertThat(rotated).isTrue();
    verify(redisTemplate).delete(accessKey("old-access-token"));
    verify(redisTemplate).delete(refreshKey("old-refresh-token"));
    verify(hashOperations).put(userKey(), "accessToken", "new-access-token");
    verify(hashOperations).put(userKey(), "refreshToken", "new-refresh-token");
    verify(redisTemplate).delete(refreshLockKey("old-refresh-token"));
  }

  @Test
  @DisplayName("JWT 교체 - 실패")
  void rotateJwtInformation_lockFail() {
    JwtInformation newJwtInformation = jwtInformation("new-access-token", "new-refresh-token");

    givenRedisValueOperations();
    given(valueOperations.setIfAbsent(eq(refreshLockKey("old-refresh-token")), eq("1"),
        any(Duration.class))).willReturn(false);

    boolean rotated = jwtRegistry.rotateJwtInformation("old-refresh-token", newJwtInformation);

    assertThat(rotated).isFalse();
  }

  private void givenRedisValueOperations() {
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
  }

  private void givenRedisHashOperations() {
    given(redisTemplate.opsForHash()).willReturn(hashOperations);
  }

  private JwtInformation jwtInformation(String accessToken, String refreshToken) {
    return new JwtInformation(
        userDto,
        accessToken,
        refreshToken,
        Instant.now().plusSeconds(600),
        Instant.now().plusSeconds(3600)
    );
  }

  private String userKey() {
    return "auth:jwt:user:" + userDto.id();
  }

  private String accessKey(String accessToken) {
    return "auth:jwt:access:" + DigestUtils.sha256Hex(accessToken);
  }

  private String refreshKey(String refreshToken) {
    return "auth:jwt:refresh:" + DigestUtils.sha256Hex(refreshToken);
  }

  private String refreshLockKey(String refreshToken) {
    return "auth:jwt:refresh-lock:" + DigestUtils.sha256Hex(refreshToken);
  }
}
