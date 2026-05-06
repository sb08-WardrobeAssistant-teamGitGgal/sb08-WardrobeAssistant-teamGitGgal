package com.gitggal.clothesplz.service.notification;

import com.gitggal.clothesplz.dto.notification.NotificationDto;

/**
 * 알림 서비스 인터페이스
 */
public interface NotificationService {

  /**
   * 알림 전송 처리
   * @param dto - 전송할 알림 데이터
   */
  void send(NotificationDto dto);
}
