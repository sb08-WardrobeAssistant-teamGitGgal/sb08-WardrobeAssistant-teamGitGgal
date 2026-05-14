package com.gitggal.clothesplz.service.user;

import com.gitggal.clothesplz.dto.user.ChangePasswordRequest;
import com.gitggal.clothesplz.dto.user.UserCreateRequest;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.mapper.user.UserMapper;
import com.gitggal.clothesplz.repository.profile.ProfileRepository;
import com.gitggal.clothesplz.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public UserDto create(UserCreateRequest request) {
    String name = request.name();
    String email = request.email();

    log.info("[Service] 회원가입 요청 시작 : name = {}", name);

    if (userRepository.existsByEmail(email)) {
      throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
    }

    String password = passwordEncoder.encode(request.password());

    User user = new User(name, email, password);

    try {
      User savedUser = userRepository.save(user);

      Profile profile = Profile.builder()
          .user(savedUser)
          .build();
      profileRepository.save(profile);

      log.info("[Service] 회원가입 요청 완료 : userId = {}", user.getId());
      return userMapper.toDto(savedUser);
    } catch (DataIntegrityViolationException e) {
      log.warn("[Service] 회원가입 요청 실패: {}", e.getMessage());
      throw new BusinessException(UserErrorCode.DUPLICATE_EMAIL);
    }
  }

  @Transactional
  @Override
  public void updatePassword(UUID userId, ChangePasswordRequest request) {
    log.info("[Service] 비밀번호 변경 요청 시작 : userId = {}", userId);

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    String newPassword = passwordEncoder.encode(request.password());
    user.updatePassword(newPassword);
    if (user.getTempPassword() != null) {
      user.clearTempPassword();
    }
    log.info("[Service] 비밀번호 변경 요청 완료 : userId = {}", userId);
  }
}
