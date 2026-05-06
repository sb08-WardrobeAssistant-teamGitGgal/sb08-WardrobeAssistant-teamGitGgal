package com.gitggal.clothesplz.service.profile.impl;

import com.gitggal.clothesplz.dto.profile.common.WeatherAPILocation;
import com.gitggal.clothesplz.dto.profile.request.ProfileUpdateRequest;
import com.gitggal.clothesplz.dto.profile.response.ProfileDto;
import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.weather.Location;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ProfileErrorCode;
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

    return profileMapper.toProfileDto(user, profile, location);
  }

  @Override
  @Transactional
  public ProfileDto updateProfile(UUID userId, ProfileUpdateRequest request, MultipartFile image) {

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

      return profileMapper.toProfileDto(user, profile, responseLocation);
    } catch (RuntimeException e) {
      if (imageUrl != null) {
        imageUploader.delete(imageUrl);
      }
      throw e;
    }
  }

  private User findUserOrThrow(UUID userId) {
    // TODO: UserNotFoundException -> 커스텀 예외 처리 해야함
    return userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
  }

  private Profile findProfileOrThrow(User user) {
    return profileRepository.findByUser(user)
        .orElseThrow(() -> new BusinessException(ProfileErrorCode.PROFILE_NOT_FOUND));
  }
}
