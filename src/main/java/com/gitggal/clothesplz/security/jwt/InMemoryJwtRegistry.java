package com.gitggal.clothesplz.security.jwt;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InMemoryJwtRegistry implements JwtRegistry {

  private final Map<UUID, JwtInformation> origin = new ConcurrentHashMap<>();
  private final Map<String, UUID> accessTokenIndex = new ConcurrentHashMap<>();
  private final Map<String, UUID> refreshTokenIndex = new ConcurrentHashMap<>();

  private final JwtTokenProvider tokenProvider;

  // JWT 등록
  @Override
  public void registerJwtInformation(JwtInformation jwtInformation) {
    UUID userId = jwtInformation.userDto().id();

    JwtInformation oldJwt = origin.get(userId);
    if (oldJwt != null) {
      removeTokenIndex(oldJwt.accessToken(), oldJwt.refreshToken());
    }

    origin.put(userId, jwtInformation);
    addTokenIndex(userId, jwtInformation.accessToken(), jwtInformation.refreshToken());
  }

  // JWT 무효화
  @Override
  public void invalidateJwtInformationByUserId(UUID userId) {
    JwtInformation info = origin.remove(userId);

    if (info != null) {
      removeTokenIndex(info.accessToken(), info.refreshToken());
    }
  }

  // JWT 존재 여부
  @Override
  public boolean hasActiveJwtInformationByUserId(UUID userId) {
    JwtInformation info = origin.get(userId);
    if (info == null) {
      return false;
    }

    boolean isValid = tokenProvider.validateAccessToken(info.accessToken()) &&
        tokenProvider.validateRefreshToken(info.refreshToken());

    if (!isValid) {
      invalidateJwtInformationByUserId(userId);
      return false;
    }

    return true;
  }

  // JWT 존재 여부 - access token
  @Override
  public boolean hasActiveJwtInformationByAccessToken(String accessToken) {
    UUID userId = accessTokenIndex.get(accessToken);
    if (userId == null) {
      return false;
    }

    return tokenProvider.validateAccessToken(accessToken);
  }

  // JWT 존재 여부 - refresh token
  @Override
  public boolean hasActiveJwtInformationByRefreshToken(String refreshToken) {
    UUID userId = refreshTokenIndex.get(refreshToken);
    if (userId == null) {
      return false;
    }

    return tokenProvider.validateRefreshToken(refreshToken);
  }

  // Jwt 토큰 갱신
  @Override
  public void rotateJwtInformation(String oldRefreshToken, JwtInformation newJwtInformation) {
    UUID userId = newJwtInformation.userDto().id();
    JwtInformation oldJwt = origin.get(userId);

    if (oldJwt != null && oldJwt.refreshToken().equals(oldRefreshToken)) {
      removeTokenIndex(oldJwt.accessToken(), oldJwt.refreshToken());
      origin.put(userId, newJwtInformation);
      addTokenIndex(userId, newJwtInformation.accessToken(), newJwtInformation.refreshToken());
    }
  }

  // 만료된 Jwt 정리
  @Override
  @Scheduled(fixedDelay = 1000 * 60 * 5)
  public void clearExpiredJwtInformation() {
    origin.entrySet().removeIf(entry -> {
      JwtInformation jwtInfo = entry.getValue();

      boolean isExpired =
          !tokenProvider.validateAccessToken(jwtInfo.accessToken()) ||
              !tokenProvider.validateRefreshToken(jwtInfo.refreshToken());

      if (isExpired) {
        removeTokenIndex(jwtInfo.accessToken(), jwtInfo.refreshToken());
      }
      return isExpired;
    });
  }

  private void addTokenIndex(UUID userId, String accessToken, String refreshToken) {
    accessTokenIndex.put(accessToken, userId);
    refreshTokenIndex.put(refreshToken, userId);
  }

  private void removeTokenIndex(String accessToken, String refreshToken) {
    accessTokenIndex.remove(accessToken);
    refreshTokenIndex.remove(refreshToken);
  }
}
