package com.gitggal.clothesplz.repository.notification;

import com.gitggal.clothesplz.entity.notification.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 알림 커스텀 Repository (QueryDSL)
 */
public interface NotificationRepositoryCustom {

  /**
   * 커서 기반 알림 목록 조회
   */
  List<Notification> findPage(
      UUID receiverId,
      Instant cursor,
      UUID idAfter,
      int limit
  );
}
