package com.gitggal.clothesplz.controller.profile.api;

import com.gitggal.clothesplz.dto.profile.request.ProfileUpdateRequest;
import com.gitggal.clothesplz.dto.profile.response.ProfileDto;
import com.gitggal.clothesplz.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "프로필 관리", description = "프로필 관련 API")
public interface ProfileControllerApi {

  @Operation(summary = "프로필 조회", description = "사용자 프로필을 조회합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "프로필 조회 성공",
      content = @Content(schema = @Schema(implementation = ProfileDto.class))
  )
  @ApiResponse(
      responseCode = "400",
      description = "프로필 조회 실패",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  ResponseEntity<ProfileDto> getProfile(
      @Parameter(description = "사용자 ID", required = true)
      UUID userId
  );

  @Operation(summary = "프로필 업데이트", description = "사용자 프로필을 수정합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "프로필 업데이트 성공",
      content = @Content(schema = @Schema(implementation = ProfileDto.class))
  )
  @ApiResponse(
      responseCode = "400",
      description = "프로필 업데이트 실패",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  @RequestBody(
      required = true,
      description = "multipart/form-data: request(JSON) + image(file)",
      content = @Content(mediaType = "multipart/form-data")
  )
  ResponseEntity<ProfileDto> updateProfile(
      @Parameter(description = "사용자 ID", required = true)
      UUID userId,
      ProfileUpdateRequest request,
      MultipartFile image
  );
}
