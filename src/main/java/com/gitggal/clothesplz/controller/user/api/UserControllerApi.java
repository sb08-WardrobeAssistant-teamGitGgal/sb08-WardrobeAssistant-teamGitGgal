package com.gitggal.clothesplz.controller.user.api;

import com.gitggal.clothesplz.dto.user.ChangePasswordRequest;
import com.gitggal.clothesplz.dto.user.UserCreateRequest;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.dto.user.UserDtoCursorRequest;
import com.gitggal.clothesplz.dto.user.UserDtoCursorResponse;
import com.gitggal.clothesplz.dto.user.UserLockUpdateRequest;
import com.gitggal.clothesplz.dto.user.UserRoleUpdateRequest;
import com.gitggal.clothesplz.exception.ErrorResponse;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

@Tag(name = "사용자 관리", description = "사용자 관련 API")
public interface UserControllerApi {

  @Operation(
      summary = "회원가입",
      description = "새로운 사용자를 생성합니다."
  )
  @ApiResponse(
      responseCode = "201",
      description = "회원가입 성공",
      content = @Content(schema = @Schema(implementation = UserDto.class))
  )
  @ApiResponse(
      responseCode = "400",
      description = "잘못된 요청 값",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  ResponseEntity<UserDto> create(
      @RequestBody(
          required = true,
          description = "회원가입 요청 정보",
          content = @Content(
              schema = @Schema(implementation = UserCreateRequest.class)
          )
      )
      @Valid
      @org.springframework.web.bind.annotation.RequestBody
      UserCreateRequest request
  );

  @Operation(
      summary = "비밀번호 변경",
      description = "사용자의 비밀번호를 변경합니다."
  )
  @ApiResponse(
      responseCode = "204",
      description = "비밀번호 변경 성공"
  )
  @ApiResponse(
      responseCode = "403",
      description = "본인 계정이 아닌 경우",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  @ApiResponse(
      responseCode = "400",
      description = "잘못된 요청 값",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  ResponseEntity<Void> changePassword(
      @Parameter(description = "사용자 ID", required = true)
      UUID userId,

      @Parameter(hidden = true)
      ClothesUserDetails principal,

      @RequestBody(
          required = true,
          description = "비밀번호 변경 요청 정보",
          content = @Content(
              schema = @Schema(implementation = ChangePasswordRequest.class)
          )
      )
      @Valid
      @org.springframework.web.bind.annotation.RequestBody
      ChangePasswordRequest request
  );

  @Operation(
      summary = "사용자 권한 변경",
      description = "관리자가 사용자의 권한을 변경합니다."
  )
  @ApiResponse(
      responseCode = "200",
      description = "권한 변경 성공",
      content = @Content(schema = @Schema(implementation = UserDto.class))
  )
  @ApiResponse(
      responseCode = "403",
      description = "관리자 권한 없음",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  @ApiResponse(
      responseCode = "400",
      description = "잘못된 요청 값",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  ResponseEntity<UserDto> updateRole(
      @Parameter(description = "사용자 ID", required = true)
      UUID userId,

      @RequestBody(
          required = true,
          description = "사용자 권한 변경 요청 정보",
          content = @Content(
              schema = @Schema(implementation = UserRoleUpdateRequest.class)
          )
      )
      @Valid
      @org.springframework.web.bind.annotation.RequestBody
      UserRoleUpdateRequest request
  );

  @Operation(
      summary = "사용자 목록 조회",
      description = "관리자가 사용자 목록을 조회합니다."
  )
  @ApiResponse(
      responseCode = "200",
      description = "사용자 목록 조회 성공",
      content = @Content(schema = @Schema(implementation = UserDtoCursorResponse.class))
  )
  @ApiResponse(
      responseCode = "403",
      description = "관리자 권한 없음",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  ResponseEntity<UserDtoCursorResponse> findAll(
      @Valid
      UserDtoCursorRequest request
  );

  @Operation(
      summary = "계정 잠금 상태 변경",
      description = "관리자가 사용자의 계정 잠금 상태를 변경합니다."
  )
  @ApiResponse(
      responseCode = "200",
      description = "계정 잠금 상태 변경 성공",
      content = @Content(schema = @Schema(implementation = UserDto.class))
  )
  @ApiResponse(
      responseCode = "403",
      description = "관리자 권한 없음",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  @ApiResponse(
      responseCode = "400",
      description = "잘못된 요청 값",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  ResponseEntity<UserDto> updateLock(
      @Parameter(description = "사용자 ID", required = true)
      UUID userId,

      @RequestBody(
          required = true,
          description = "계정 잠금 상태 변경 요청 정보",
          content = @Content(
              schema = @Schema(implementation = UserLockUpdateRequest.class)
          )
      )
      @org.springframework.web.bind.annotation.RequestBody
      UserLockUpdateRequest request
  );
}