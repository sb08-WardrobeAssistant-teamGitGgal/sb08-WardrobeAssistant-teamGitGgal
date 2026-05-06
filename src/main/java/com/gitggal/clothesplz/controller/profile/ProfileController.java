package com.gitggal.clothesplz.controller.profile;

import com.gitggal.clothesplz.controller.profile.api.ProfileControllerApi;
import com.gitggal.clothesplz.dto.profile.request.ProfileUpdateRequest;
import com.gitggal.clothesplz.dto.profile.response.ProfileDto;
import com.gitggal.clothesplz.service.profile.ProfileService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ProfileController implements ProfileControllerApi {

  private final ProfileService profileService;

  @Override
  @GetMapping("/{userId}/profiles")
  public ResponseEntity<ProfileDto> getProfile(
      @PathVariable UUID userId
  ) {
    // TODO: Auth작업 끝나면 본인만 조회 or 관리자 조회 체크 해야함

    ProfileDto response = profileService.getProfile(userId);

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(response);
  }

  @Override
  @PatchMapping(value = "/{userId}/profiles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ProfileDto> updateProfile(
      @PathVariable UUID userId,
      @RequestPart("request") @Valid ProfileUpdateRequest request,
      @RequestPart(value = "image", required = false) MultipartFile image
  ) {
    // TODO: Auth작업 끝나면 본인수정 or 관리자수정 체크 해야함

    ProfileDto response = profileService.updateProfile(userId, request, image);

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(response);
  }
}
