package com.gitggal.clothesplz.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetMailSender {

  private final JavaMailSender javaMailSender;

  @Value("${spring.mail.username}")
  private String senderEmail;

  @Async("taskExecutor")
  public void sendTempPasswordEmail(String email, String tempPassword) {
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(senderEmail);
      message.setTo(email);
      message.setSubject("[옷장을 부탁해] 임시 비밀번호 발급");
      message.setText("안녕하세요. 옷장을 부탁해입니다. \n 임시 비밀번호가 발급되었습니다. "
          + "\n 임시 비밀번호 : " + tempPassword + " \n3분 뒤 임시 비밀번호는 파기됩니다.\n "
          + "로그인 후 마이페이지에서 비밀번호를 변경해 주세요");
      javaMailSender.send(message);
      log.info("[Service] 임시 비밀번호 이메일 전송 완료: email={}", email);
    } catch (Exception e) {
      log.warn("[Service] 임시 비밀번호 이메일 전송 실패: email={}, message={}", email, e.getMessage());
    }
  }
}
