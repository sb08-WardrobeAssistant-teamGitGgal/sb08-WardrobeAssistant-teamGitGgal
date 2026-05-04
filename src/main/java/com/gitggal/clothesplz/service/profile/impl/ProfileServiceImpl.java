package com.gitggal.clothesplz.service.profile.impl;

import com.gitggal.clothesplz.dto.profile.request.ProfileUpdateRequest;
import com.gitggal.clothesplz.dto.profile.response.ProfileDto;
import com.gitggal.clothesplz.repository.profile.ProfileRepository;
import com.gitggal.clothesplz.service.profile.ProfileService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileServiceImpl implements ProfileService {

  private final ProfileRepository profileRepository;

  @Override
  public ProfileDto getProfile(UUID userId) {
    return null;
  }

  @Override
  public ProfileDto updateProfile(UUID userId, ProfileUpdateRequest request, MultipartFile image) {
    return null;
  }
}
