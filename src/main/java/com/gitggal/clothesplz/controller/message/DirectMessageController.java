package com.gitggal.clothesplz.controller.message;

import com.gitggal.clothesplz.dto.message.DirectMessageDtoCursorResponse;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import com.gitggal.clothesplz.service.message.DirectMessageService;
import com.gitggal.clothesplz.util.AuthenticationUtil;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * DM REST 컨트롤러
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/direct-messages")
@RequiredArgsConstructor
public class DirectMessageController {

  private final DirectMessageService directMessageService;

  @GetMapping
  public ResponseEntity<DirectMessageDtoCursorResponse> getMessages(
      @RequestParam UUID userId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam @Positive @Max(100) int limit,
      @AuthenticationPrincipal ClothesUserDetails userDetails) {

    // 인증된 유저
    UUID myId = AuthenticationUtil.extractUserId(userDetails);

    log.info("[Controller] DM 목록 조회 요청 시작: myId={}, partnerId={}", myId, userId);

    DirectMessageDtoCursorResponse response =
        directMessageService.getMessages(myId, userId, cursor, idAfter, limit);

    log.info("[Controller] DM 목록 조회 요청 완료");

    return ResponseEntity.ok(response);
  }
}
