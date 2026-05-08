package com.gitggal.clothesplz.service.notification;

import com.gitggal.clothesplz.dto.notification.NotificationDtoCursorResponse;
import com.gitggal.clothesplz.dto.notification.NotificationRequest;
import java.util.UUID;

/**
 * 알림 서비스 인터페이스
 */
public interface NotificationService {

  /**
   * 알림 전송 처리
   * @param request - 전송할 알림 데이터
   */
  void send(NotificationRequest request);

  /**
   * 알림 목록 조회 (커서 기반 페이지네이션)
   * @param receiverId - 로그인한 사용자 ID
   * @param cursor     - 이전 페이지 마지막 createdAt
   * @param idAfter    - 이전 페이지 마지막 id
   * @param limit      - 한 페이지 크기
   */
  NotificationDtoCursorResponse getNotifications(UUID receiverId, String cursor, UUID idAfter, int limit);

  /**
   * 알림 읽음 처리 (삭제)
   * @param notificationId - 알림 ID
   * @param requesterId    - 요청한 사용자 ID
   */
  void deleteNotification(UUID notificationId, UUID requesterId);
}
