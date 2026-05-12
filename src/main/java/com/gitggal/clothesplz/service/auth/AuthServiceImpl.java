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
import jakarta.transaction.Transactional;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final JwtRegistry jwtRegistry;
  private final JwtTokenProvider tokenProvider;
  private final ClothesUserDetailsService clothesUserDetailsService;
  private final PasswordEncoder passwordEncoder;
  private final UserRepository userRepository;
  private final JavaMailSender javaMailSender;

  @Value("${spring.mail.username}")
  private String senderEmail;

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

  @Transactional
  @Override
  public void sendTempPassword(ResetPasswordRequest request) {
    log.info("[Service] 임시 비밀번호 발급 요청");
    String email = request.email();

    log.info("이메일 전송 시작");
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    String tempPassword;

    // 임시 비밀번호 생성
    try {
      SecureRandom secureRandom = SecureRandom.getInstance("NativePRNG");
      byte[] bytes = new byte[10];
      secureRandom.nextBytes(bytes);
      tempPassword = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
      String encodeTempPassword = passwordEncoder.encode(tempPassword);
      user.updateTempPassword(encodeTempPassword);
    } catch (NoSuchAlgorithmException e) {
      throw new BusinessException(UserErrorCode.JWT_TOKEN_GENERATION_FAILED);
    }

    // 메일 전송
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(senderEmail);
    message.setTo(email);
    message.setSubject("[옷장을 부탁해] 임시 비밀번호 발급");
    message.setText("안녕하세요. 옷장을 부탁해입니다. \n 임시 비밀번호가 발급되었습니다. "
        + "\n 임시 비밀번호 : " + tempPassword + " \n3분 뒤 임시 비밀번호는 파기됩니다.\n "
        + "로그인 후 마이페이지에서 비밀번호를 변경해 주세요");
    javaMailSender.send(message);
    log.info("[Service] 임시 비밀번호 발급 완료");
  }
}
