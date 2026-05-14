package com.gitggal.clothesplz.controller.user;

import com.gitggal.clothesplz.dto.user.ChangePasswordRequest;
import com.gitggal.clothesplz.dto.user.UserCreateRequest;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.dto.user.UserRoleUpdateRequest;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import com.gitggal.clothesplz.service.user.UserService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  @PostMapping
  public ResponseEntity<UserDto> create(@Validated @RequestBody UserCreateRequest request) {
    log.info("[Controller] 회원가입 요청 시작");
    UserDto dto = userService.create(request);
    log.info("[Controller] 회원가입 요청 완료");
    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
  }

  @PatchMapping("/{userId}/password")
  public ResponseEntity<Void> changePassword(
      @PathVariable UUID userId,
      @AuthenticationPrincipal ClothesUserDetails principal,
      @Validated @RequestBody ChangePasswordRequest request) {
    log.info("[Controller] 비밀번호 변경 요청 시작");
    if (!principal.getUserDto().id().equals(userId)) {
      throw new BusinessException(UserErrorCode.FORBIDDEN);
    }
    userService.updatePassword(userId, request);
    log.info("[Controller] 비밀번호 변경 요청 완료");
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/{userId}/role")
  public ResponseEntity<UserDto> updateRole(
      @PathVariable UUID userId,
      @Validated @RequestBody UserRoleUpdateRequest request) {
    log.info("[Controller] 권한 변경 요청 시작");
    UserDto dto = userService.updateRole(userId, request);
    log.info("[Controller] 권한 변경 요청 완료");
    return ResponseEntity.status(HttpStatus.OK).body(dto);
  }

}
