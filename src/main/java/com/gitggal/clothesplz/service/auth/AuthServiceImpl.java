package com.gitggal.clothesplz.service.auth;

import com.gitggal.clothesplz.dto.user.ResetPasswordRequest;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.repository.user.UserRepository;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import com.gitggal.clothesplz.security.ClothesUserDetailsService;
import com.gitggal.clothesplz.security.jwt.JwtInformation;
import com.gitggal.clothesplz.security.jwt.JwtRegistry;
import com.gitggal.clothesplz.security.jwt.JwtTokenProvider;
import com.nimbusds.jose.JOSEException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final JwtRegistry jwtRegistry;
  private final JwtTokenProvider tokenProvider;
  private final ClothesUserDetailsService clothesUserDetailsService;
  private final PasswordEncoder passwordEncoder;
  private final UserRepository userRepository;
  private final PasswordResetMailSender passwordResetMailSender;

  @Override
  public JwtInformation refresh(String refreshToken) {
    if (!tokenProvider.validateRefreshToken(refreshToken)) {
      throw new BusinessException(UserErrorCode.JWT_TOKEN_INVALID);
    }

    if (!jwtRegistry.hasActiveJwtInformationByRefreshToken(refreshToken)) {
      throw new BusinessException(UserErrorCode.JWT_TOKEN_EXPIRED);
    }

    String userId = tokenProvider.getUsernameFromToken(refreshToken);
    if (userId == null || userId.isBlank()) {
      throw new BusinessException(UserErrorCode.JWT_TOKEN_INVALID);
    }

    UUID parsedUserId;
    try {
      parsedUserId = UUID.fromString(userId);
    } catch (IllegalArgumentException e) {
      throw new BusinessException(UserErrorCode.JWT_TOKEN_INVALID);
    }

    UserDetails userDetails = clothesUserDetailsService.loadUserById(parsedUserId);

    if (!(userDetails instanceof ClothesUserDetails clothesUserDetails)) {
      throw new BusinessException(UserErrorCode.JWT_TOKEN_INVALID);
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
      boolean rotated = jwtRegistry.rotateJwtInformation(
          refreshToken,
          jwtInformation
      );

      if (!rotated) {
        throw new BusinessException(UserErrorCode.JWT_TOKEN_INVALID);
      }

      return jwtInformation;
    } catch (JOSEException e) {
      throw new BusinessException(UserErrorCode.JWT_TOKEN_GENERATION_FAILED);
    }

  }

  @Transactional
  @Override
  public void sendTempPassword(ResetPasswordRequest request) {
    log.info("[Service] 임시 비밀번호 발급 요청");
    String email = request.email();

    // 이메일을 통해서 해킹 시도할 수 있으므로 예외 X
    User user = userRepository.findByEmail(email).orElse(null);

    if (user == null) {
      log.warn("[Service] 존재하지 않는 이메일 요청");
      return;
    }

    String tempPassword;

    // 임시 비밀번호 생성
    try {
      SecureRandom secureRandom = new SecureRandom();
      byte[] bytes = new byte[10];
      secureRandom.nextBytes(bytes);
      tempPassword = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
      String encodeTempPassword = passwordEncoder.encode(tempPassword);
      user.updateTempPassword(encodeTempPassword);
    } catch (Exception e) {
      log.warn("[Service] 임시 비밀번호 발급 실패: message = {}", e.getMessage());
      throw new BusinessException(UserErrorCode.TEMP_PASSWORD_GENERATION_FAILED);
    }

    final String finalTempPassword = tempPassword;
    runAfterCommit(() -> passwordResetMailSender.sendTempPasswordEmail(email, finalTempPassword,
        () -> clearTempPasswordById(user.getId())));
  }

  private void runAfterCommit(Runnable task) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      task.run();
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            task.run();
          }
        }
    );
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void clearTempPasswordById(UUID userId) {
    userRepository.findById(userId)
        .ifPresent(user -> {
          user.clearTempPassword();
          log.info("[Service] 메일 전송 실패로 임시 비밀번호 초기화: userId={}", userId);
        });
  }
}
