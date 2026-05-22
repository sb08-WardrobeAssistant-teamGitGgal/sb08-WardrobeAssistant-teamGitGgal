package com.gitggal.clothesplz.service.user;

import com.gitggal.clothesplz.dto.user.ChangePasswordRequest;
import com.gitggal.clothesplz.dto.user.UserCreateRequest;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.dto.user.UserDtoCursorRequest;
import com.gitggal.clothesplz.dto.user.UserDtoCursorResponse;
import com.gitggal.clothesplz.dto.user.UserLockUpdateRequest;
import com.gitggal.clothesplz.dto.user.UserRoleUpdateRequest;
import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.user.SocialAccount;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.event.user.PermissionChangedEvent;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.mapper.user.UserMapper;
import com.gitggal.clothesplz.repository.profile.ProfileRepository;
import com.gitggal.clothesplz.repository.user.SocialAccountRepository;
import com.gitggal.clothesplz.repository.user.UserRepository;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import com.gitggal.clothesplz.security.jwt.JwtRegistry;
import com.gitggal.clothesplz.security.oauth.OAuthInformation;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final JwtRegistry jwtRegistry;
  private final SocialAccountRepository socialAccountRepository;
  private final ApplicationEventPublisher eventPublisher;

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

    User user = findUser(userId);

    String newPassword = passwordEncoder.encode(request.password());
    user.updatePassword(newPassword);
    if (user.getTempPassword() != null) {
      user.clearTempPassword();
    }
    log.info("[Service] 비밀번호 변경 요청 완료 : userId = {}", userId);
  }

  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  @Override
  public UserDto updateRole(UUID userId, UserRoleUpdateRequest request) {
    log.info("[Service] 권한 변경 요청 시작 : userId = {}", userId);

    User user = findUser(userId);

    if (user.getRole() == request.role()) {
      log.info("[Service] 권한 변경 요청 실패: 같은 역할 userId={}", userId);
      return userMapper.toDto(user);
    }

    user.updateRole(request.role());

    jwtRegistry.invalidateJwtInformationByUserId(userId);

    eventPublisher.publishEvent(new PermissionChangedEvent(
        userId,
        request.role()
    ));

    log.info("[Service] 권한 변경 요청 완료 : userId = {}", userId);
    return userMapper.toDto(user);
  }

  @Transactional(readOnly = true)
  @Override
  public UserDtoCursorResponse findAll(UserDtoCursorRequest request) {
    log.info("[Service] 목록 조회 요청 시작");
    List<User> users = userRepository.getAllUsers(request);

    boolean hasNext = users.size() > request.limit();

    if (hasNext) {
      users = users.subList(0, request.limit());
    }

    List<UserDto> userDtos = users.stream()
        .map(userMapper::toDto)
        .toList();

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (hasNext && !users.isEmpty()) {
      User lastUser = users.get(users.size() - 1);

      if ("email".equalsIgnoreCase(request.sortBy())) {
        nextCursor = lastUser.getEmail();
      } else if ("createdAt".equalsIgnoreCase(request.sortBy())) {
        nextCursor = lastUser.getCreatedAt().toString();
      }

      nextIdAfter = lastUser.getId();
    }

    long totalCount = userRepository.totalCount(request);

    log.info("[Service] 목록 조회 요청 완료");
    return new UserDtoCursorResponse(userDtos, nextCursor, nextIdAfter, hasNext, totalCount,
        request.sortBy(), request.sortDirection());
  }


  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  @Override
  public UserDto updateLock(UUID userId, UserLockUpdateRequest request) {
    log.info("[Service] 계정 잠금 상태 변경 요청 시작 : userId = {}", userId);

    User user = findUser(userId);

    user.updateLock(request.locked());

    if (request.locked()) {
      jwtRegistry.invalidateJwtInformationByUserId(userId);
    }

    log.info("[Service] 계정 잠금 상태 변경 요청 완료 : userId = {}", userId);
    return userMapper.toDto(user);
  }

  private User findUser(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
  }

  @Transactional
  @Override
  public ClothesUserDetails processOAuth2User(OAuthInformation info) {
    log.info("[Service] OAuth 사용자 처리 요청 시작");

    User user = socialAccountRepository
        .findByProviderAndProviderId(info.provider(), info.providerId())
        .map(SocialAccount::getUser)
        .orElseGet(() -> {
          User existingUser = userRepository.findByEmail(info.email())
              .orElseGet(() -> createOAuthUser(info));

          SocialAccount socialAccount = new SocialAccount(
              existingUser,
              info.provider(),
              info.providerId()
          );
          socialAccountRepository.save(socialAccount);

          log.info("[Service] 소셜 계정 연결 완료");

          return existingUser;
        });

    if (info.nickname() != null && !info.nickname().equals(user.getName())) {
      user.updateName(info.nickname());
    }

    log.info("[Service] OAuth 사용자 처리 요청 완료");

    return new ClothesUserDetails(
        userMapper.toDto(user),
        user.getPassword(),
        user.getTempPassword(),
        user.getTempPasswordExpiresAt()
    );
  }

  private User createOAuthUser(OAuthInformation info) {
    log.info("[Service] OAuth 사용자 생성 요청 시작");

    User user = new User(
        info.nickname(),
        info.email(),
        passwordEncoder.encode(UUID.randomUUID().toString())
    );
    User savedUser = userRepository.save(user);

    Profile profile = Profile.builder()
        .user(savedUser)
        .build();
    profileRepository.save(profile);

    SocialAccount socialAccount = new SocialAccount(savedUser, info.provider(), info.providerId());
    socialAccountRepository.save(socialAccount);

    log.info("[Service] OAuth 사용자 생성 요청 완료");
    return savedUser;
  }
}
