package com.gitggal.clothesplz.service.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetMailSender Test")
class PasswordResetMailSenderTest {

  @InjectMocks
  private PasswordResetMailSender passwordResetMailSender;

  @Mock
  private JavaMailSender javaMailSender;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(
        passwordResetMailSender,
        "senderEmail",
        "sender@test.com"
    );
  }

  @Test
  @DisplayName("임시 비밀번호 메일 발송 성공")
  void sendTempPasswordEmail_success() {
    // given
    Runnable onFailure = mock(Runnable.class);

    // when
    passwordResetMailSender.sendTempPasswordEmail(
        "user@test.com",
        "tempPassword",
        onFailure
    );

    // then
    then(javaMailSender).should().send(any(SimpleMailMessage.class));
    then(onFailure).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("임시 비밀번호 메일 발송 실패 시 rollback 실행")
  void sendTempPasswordEmail_fail() {
    // given
    Runnable onFailure = mock(Runnable.class);

    doThrow(new MailSendException("메일 전송 실패"))
        .when(javaMailSender)
        .send(any(SimpleMailMessage.class));

    // when
    passwordResetMailSender.sendTempPasswordEmail(
        "user@test.com",
        "tempPassword",
        onFailure
    );

    // then
    verify(onFailure).run();
  }
}
