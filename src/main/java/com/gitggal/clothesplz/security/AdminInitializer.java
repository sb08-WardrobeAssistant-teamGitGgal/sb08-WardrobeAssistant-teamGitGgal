package com.gitggal.clothesplz.security;

import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.user.UserRole;
import com.gitggal.clothesplz.repository.profile.ProfileRepository;
import com.gitggal.clothesplz.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

  @Value("${clothesplz.admin.username}")
  private String name;

  @Value("${clothesplz.admin.password}")
  private String password;

  @Value("${clothesplz.admin.email}")
  private String email;

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  @Override
  public void run(ApplicationArguments args) {
    log.info("[AdminInitializer] 관리자 계정 생성 요청");

    if (userRepository.existsByEmail(email)) {
      log.info("[AdminInitializer] 관리자 계정 생성 실패 : 이미 계정이 존재합니다.");
      return;
    }

    String encodePassword = passwordEncoder.encode(password);

    User admin = new User(name, email, encodePassword);
    admin.updateRole(UserRole.ADMIN);
    userRepository.save(admin);

    Profile profile = Profile.builder()
        .user(admin)
        .build();
    profileRepository.save(profile);
    log.info("[AdminInitializer] 관리자 계정 생성 완료");
  }
}
