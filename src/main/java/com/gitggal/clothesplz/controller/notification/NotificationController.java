package com.gitggal.clothesplz.controller.notification;

import com.gitggal.clothesplz.dto.notification.NotificationDtoCursorResponse;
import com.gitggal.clothesplz.service.notification.NotificationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 관련 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  /**
   * 알림 목록 조회 (커서 기반 페이지네이션)
   */
  @GetMapping
  public ResponseEntity<NotificationDtoCursorResponse> getNotifications(
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam int limit,
      @RequestParam UUID receiverId) {

    log.info("[Controller] 알림 목록 조회 요청 시작: receiverId={}, cursor={}, idAfter={}, limit={}", receiverId, cursor, idAfter, limit);

    NotificationDtoCursorResponse response =
        notificationService.getNotifications(receiverId, cursor, idAfter, limit);

    log.info("[Controller] 알림 목록 조회 요청 완료: receiverId={}", receiverId);

    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{notificationId}")
  public ResponseEntity<Void> deleteNotification(
      @PathVariable UUID notificationId,
      @RequestParam UUID requesterId) {

    log.info("[Controller] 알림 읽음 처리 요청 시작: notificationId={}, requesterId={}", notificationId, requesterId);

    notificationService.deleteNotification(notificationId, requesterId);

    log.info("[Controller] 알림 읽음 처리 요청 완료: notificationId={}", notificationId);

    return ResponseEntity.noContent().build();
  }
}
