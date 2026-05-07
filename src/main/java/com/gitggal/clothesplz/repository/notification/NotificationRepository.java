package com.gitggal.clothesplz.repository.notification;

import com.gitggal.clothesplz.entity.notification.Notification;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 알림 Repository 인터페이스
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationRepositoryCustom {

  /**
   * 사용자 전체 알림 수
   */
  long countByReceiver_Id(UUID receiverId);
}
