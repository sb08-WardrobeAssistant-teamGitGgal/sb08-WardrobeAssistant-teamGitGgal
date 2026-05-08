package com.gitggal.clothesplz.security.jwt;

import com.gitggal.clothesplz.dto.user.UserDto;
import java.time.Instant;

public record JwtInformation(
    UserDto userDto,
    String accessToken,
    String refreshToken,
    Instant accessTokenExpiry,
    Instant refreshTokenExpiry
) {

  public JwtInformation rotate(String newAccessToken, String newRefreshToken, Instant newAccessTokenExpiry, Instant newRefreshTokenExpiry
  ) {
    return new JwtInformation(
        this.userDto,
        newAccessToken,
        newRefreshToken,
        newAccessTokenExpiry,
        newRefreshTokenExpiry
    );
  }

  public boolean isAccessTokenExpired() {
    return accessTokenExpiry.isBefore(Instant.now());
  }

  public boolean isRefreshTokenExpired() {
    return refreshTokenExpiry.isBefore(Instant.now());
  }
}
