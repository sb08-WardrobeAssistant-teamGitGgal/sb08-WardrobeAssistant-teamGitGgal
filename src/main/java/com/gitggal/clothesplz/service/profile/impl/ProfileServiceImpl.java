package com.gitggal.clothesplz.service.profile.impl;

import com.gitggal.clothesplz.dto.profile.common.WeatherAPILocation;
import com.gitggal.clothesplz.dto.profile.request.ProfileUpdateRequest;
import com.gitggal.clothesplz.dto.profile.response.ProfileDto;
import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.weather.Location;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ProfileErrorCode;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.mapper.profile.ProfileMapper;
import com.gitggal.clothesplz.repository.profile.ProfileRepository;
import com.gitggal.clothesplz.repository.user.UserRepository;
import com.gitggal.clothesplz.repository.weather.LocationRepository;
import com.gitggal.clothesplz.service.image.ImageUploader;
import com.gitggal.clothesplz.service.profile.ProfileService;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

  private final ProfileRepository profileRepository;
  private final UserRepository userRepository;
  private final LocationRepository locationRepository;
  private final ProfileMapper profileMapper;
  private final ImageUploader imageUploader;

  @Override
  @Transactional(readOnly = true)
  public ProfileDto getProfile(UUID userId) {
    log.info("[Service] 프로필 조회 요청 시작: 조회 요청 userId = {}", userId);

    User user = findUserOrThrow(userId);
    Profile profile = findProfileOrThrow(user);

    WeatherAPILocation location;

    if (profile.getGridX() == null || profile.getGridY() == null) {
      location = WeatherAPILocation.of(null, null, null, null, List.of());
    } else {
      location = locationRepository.findByGridXAndGridY(
              profile.getGridX(),
              profile.getGridY()
          )
          .map(profileMapper::toWeatherAPILocation)
          .orElse(WeatherAPILocation.of(
              profile.getLatitude(),
              profile.getLongitude(),
              profile.getGridX(),
              profile.getGridY(),
              List.of())
          );
    }

    log.info("[Service] 프로필 조회 요청 완료");
    return profileMapper.toProfileDto(user, profile, location);
  }

  @Override
  @Transactional
  public ProfileDto updateProfile(UUID userId, ProfileUpdateRequest request, MultipartFile image) {
    log.info("[Service] 프로필 수정 요청 시작: 수정 요청 userId = {}, userName = {}", userId, request.name());

    User user = findUserOrThrow(userId);
    Profile profile = findProfileOrThrow(user);

    // TODO: User 이름 변경 정책 확정 후 반영해야 합니다.
    // e.g.
    // if (request.name() != null) {
    //  user.updateName(request.name());
    // }

    String imageUrl = (image != null && !image.isEmpty())
        ? imageUploader.upload(image)
        : null;

    try {
      Location location = locationRepository.findByGridXAndGridY(
          request.location().x(),
          request.location().y()
      ).orElseGet(() ->
          locationRepository.save(Location.builder()
              .latitude(request.location().latitude())
              .longitude(request.location().longitude())
              .gridX(request.location().x())
              .gridY(request.location().y())
              .locationNames(request.location().locationNames().stream()
                  .map(String::trim)
                  .collect(Collectors.joining(","))
              )
              .build())
      );

      WeatherAPILocation responseLocation = profileMapper.toWeatherAPILocation(location);

      String oldImageUrl = profile.getImageUrl();

      profile.update(
          request.gender(),
          imageUrl,
          request.birthDate(),
          location.getLatitude(),
          location.getLongitude(),
          location.getGridX(),
          location.getGridY(),
          request.temperatureSensitivity()
      );

      deleteOldImageAfterCommit(imageUrl, oldImageUrl);

      log.info("[Service] 프로필 수정 요청 완료");
      return profileMapper.toProfileDto(user, profile, responseLocation);
    } catch (RuntimeException e) {
      log.error("[Service] 프로필 수정 요청 실패");
      if (imageUrl != null) {
        imageUploader.delete(imageUrl);
      }
      throw e;
    }
  }

  private User findUserOrThrow(UUID userId) {
    log.warn("[Service] 사용자 조회 실패: 존재하지 않는 사용자");
    return userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
  }

  private Profile findProfileOrThrow(User user) {
    log.warn("[Service] 프로필 조회 실패: 존재하지 않는 프로필");
    return profileRepository.findByUser(user)
        .orElseThrow(() -> new BusinessException(ProfileErrorCode.PROFILE_NOT_FOUND));
  }

  private void deleteOldImageAfterCommit(String imageUrl, String oldImageUrl) {
    if (imageUrl == null || oldImageUrl == null || oldImageUrl.equals(imageUrl)) {
      return;
    }

    // 커밋이 완전히 끝나고, 기존 이미지 제거를 위함
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        imageUploader.delete(oldImageUrl);
      }
    });
  }
}
