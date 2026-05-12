package com.gitggal.clothesplz.service.auth;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import com.gitggal.clothesplz.security.ClothesUserDetailsService;
import com.gitggal.clothesplz.security.jwt.JwtInformation;
import com.gitggal.clothesplz.security.jwt.JwtRegistry;
import com.gitggal.clothesplz.security.jwt.JwtTokenProvider;
import com.nimbusds.jose.JOSEException;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final JwtRegistry jwtRegistry;
  private final JwtTokenProvider tokenProvider;
  private final ClothesUserDetailsService clothesUserDetailsService;

  @Override
  public JwtInformation refresh(String refreshToken) {
    if (!tokenProvider.validateRefreshToken(refreshToken)
        || !jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)) {
      throw new BusinessException(UserErrorCode.INVALID_TOKEN);
    }

    String userId = tokenProvider.getUsernameFromToken(refreshToken);
    if (userId == null || userId.isBlank()) {
      throw new BusinessException(UserErrorCode.INVALID_TOKEN);
    }

    UUID parsedUserId;
    try {
      parsedUserId = UUID.fromString(userId);
    } catch (IllegalArgumentException e) {
      throw new BusinessException(UserErrorCode.INVALID_TOKEN);
    }

    UserDetails userDetails = clothesUserDetailsService.loadUserById(parsedUserId);

    if (!(userDetails instanceof ClothesUserDetails clothesUserDetails)) {
      throw new BusinessException(UserErrorCode.INVALID_TOKEN);
    }

    try {
      String newAccessToken = tokenProvider.generateAccessToken(clothesUserDetails);
      String newRefreshToken = tokenProvider.generateRefreshToken(clothesUserDetails);
      Instant newAccessTokenExpiry = tokenProvider.getAccessTokenExpiry(newAccessToken);
      Instant newRefreshTokenExpiry = tokenProvider.getRefreshTokenExpiry(newRefreshToken);

      JwtInformation jwtInformation =
          new JwtInformation(
              clothesUserDetails.getUserDto(),
              newAccessToken,
              newRefreshToken,
              newAccessTokenExpiry,
              newRefreshTokenExpiry
          );
      jwtRegistry.rotateJwtInformation(
          refreshToken,
          jwtInformation
      );
      return jwtInformation;
    } catch (JOSEException e) {
      throw new BusinessException(UserErrorCode.JWT_TOKEN_GENERATION_FAILED);
    }

  }
}
