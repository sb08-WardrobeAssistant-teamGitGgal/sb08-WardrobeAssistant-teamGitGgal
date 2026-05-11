package com.gitggal.clothesplz.service.profile;

import com.gitggal.clothesplz.dto.profile.request.ProfileUpdateRequest;
import com.gitggal.clothesplz.dto.profile.response.ProfileDto;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {

  ProfileDto getProfile(UUID userId);

  ProfileDto updateProfile(UUID userId, ProfileUpdateRequest request, MultipartFile image);
}
