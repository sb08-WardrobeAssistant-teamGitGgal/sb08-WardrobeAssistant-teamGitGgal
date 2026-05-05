package com.gitggal.clothesplz.service.profile.impl;

import com.gitggal.clothesplz.dto.profile.common.WeatherAPILocation;
import com.gitggal.clothesplz.dto.profile.request.ProfileUpdateRequest;
import com.gitggal.clothesplz.dto.profile.response.ProfileDto;
import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.exception.code.ProfileErrorCode;
import com.gitggal.clothesplz.exception.profile.ProfileNotFoundException;
import com.gitggal.clothesplz.mapper.profile.ProfileMapper;
import com.gitggal.clothesplz.repository.profile.ProfileRepository;
import com.gitggal.clothesplz.repository.user.UserRepository;
import com.gitggal.clothesplz.repository.weather.LocationRepository;
import com.gitggal.clothesplz.service.profile.ProfileService;
import java.util.List;
import java.util.UUID;
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

  @Override
  @Transactional(readOnly = true)
  public ProfileDto getProfile(UUID userId) {

    // TODO: UserNotFoundException -> 커스텀 예외 처리 해야함
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new IllegalArgumentException("User not found"));

    Profile profile = profileRepository.findByUser(user)
        .orElseThrow(() -> new ProfileNotFoundException(ProfileErrorCode.PROFILE_NOT_FOUND));

    // TODO: Locations Entity랑 Repository 구조 완성되면 repository 사용
    WeatherAPILocation location = WeatherAPILocation.of(
        profile.getLatitude(),
        profile.getLongitude(),
        profile.getGridX(),
        profile.getGridY(),
        List.of()
    );

    return profileMapper.toDtoForGetProfile(user, profile, location);
  }

  @Override
  @Transactional
  public ProfileDto updateProfile(UUID userId, ProfileUpdateRequest request, MultipartFile image) {
    return null;
  }
}
