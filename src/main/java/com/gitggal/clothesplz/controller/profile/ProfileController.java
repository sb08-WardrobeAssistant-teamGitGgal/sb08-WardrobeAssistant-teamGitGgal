package com.gitggal.clothesplz.controller.profile;

import com.gitggal.clothesplz.controller.profile.api.ProfileControllerApi;
import com.gitggal.clothesplz.dto.profile.request.ProfileUpdateRequest;
import com.gitggal.clothesplz.dto.profile.response.ProfileDto;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import com.gitggal.clothesplz.service.profile.ProfileService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@Slf4j
public class ProfileController implements ProfileControllerApi {

  private final ProfileService profileService;

  @Override
  @GetMapping("/{userId}/profiles")
  @PreAuthorize("#userDetails.userDto.id == #userId or hasRole('ADMIN')")
  public ResponseEntity<ProfileDto> getProfile(
      @PathVariable UUID userId,
      @AuthenticationPrincipal ClothesUserDetails userDetails
  ) {
    log.info("[Controller] 프로필 조회 요청 시작: 조회 요청 userId = {}", userId);

    ProfileDto response = profileService.getProfile(userId);

    log.info("[Controller] 프로필 조회 요청 완료");
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(response);
  }

  @Override
  @PatchMapping(value = "/{userId}/profiles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("#userDetails.userDto.id == #userId or hasRole('ADMIN')")
  public ResponseEntity<ProfileDto> updateProfile(
      @PathVariable UUID userId,
      @RequestPart("request") @Valid ProfileUpdateRequest request,
      @RequestPart(value = "image", required = false) MultipartFile image,
      @AuthenticationPrincipal ClothesUserDetails userDetails
  ) {
    log.info("[Controller] 프로필 수정 요청 시작: 수정 요청 userId = {}", userId);

    ProfileDto response = profileService.updateProfile(userId, request, image);

    log.info("[Controller] 프로필 수정 요청 완료");
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(response);
  }
}
