package com.gitggal.clothesplz.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.user.UserRole;
import com.gitggal.clothesplz.repository.profile.ProfileRepository;
import com.gitggal.clothesplz.repository.user.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminInitializer Test")
class AdminInitializerTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private ProfileRepository profileRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private ApplicationArguments args;

  @InjectMocks
  private AdminInitializer adminInitializer;

  private final String adminName = "Admin";
  private final String adminEmail = "admin@clothesplz.com";
  private final String adminPassword = "admin1234!";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(adminInitializer, "name", adminName);
    ReflectionTestUtils.setField(adminInitializer, "email", adminEmail);
    ReflectionTestUtils.setField(adminInitializer, "password", adminPassword);
  }

  @Test
  @DisplayName("관리자 계정 생성 성공")
  void createAdminAccount_success() throws Exception {
    // given
    given(userRepository.existsByEmail(adminEmail)).willReturn(false);
    given(passwordEncoder.encode(adminPassword)).willReturn("encoded_password");

    User savedAdmin = new User(adminName, adminEmail, "encoded_password");
    ReflectionTestUtils.setField(savedAdmin, "id", UUID.randomUUID());
    savedAdmin.updateRole(UserRole.ADMIN);
    Profile profile = Profile.builder().user(savedAdmin).build();
    given(userRepository.save(any(User.class))).willReturn(savedAdmin);
    given(profileRepository.save(any(Profile.class))).willReturn(profile);

    // when
    adminInitializer.run(args);

    // then
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(userCaptor.capture());

    User capturedUser = userCaptor.getValue();
    assertThat(capturedUser.getName()).isEqualTo(adminName);
    assertThat(capturedUser.getEmail()).isEqualTo(adminEmail);
    assertThat(capturedUser.getRole()).isEqualTo(UserRole.ADMIN);

    verify(passwordEncoder).encode(adminPassword);
    verify(profileRepository).save(any(Profile.class));
  }

  @Test
  @DisplayName("관리자 계정 생성 실패 - 이미 존재")
  void createAdminAccount_alreadyExists() throws Exception {
    // given
    given(userRepository.existsByEmail(adminEmail)).willReturn(true);

    // when
    adminInitializer.run(args);

    // then
    verify(userRepository, never()).save(any(User.class));
    verify(profileRepository, never()).save(any(Profile.class));
    verify(passwordEncoder, never()).encode(anyString());
  }
}
