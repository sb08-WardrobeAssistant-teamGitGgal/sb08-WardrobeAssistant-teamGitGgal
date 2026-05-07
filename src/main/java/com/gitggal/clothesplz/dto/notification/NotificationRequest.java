package com.gitggal.clothesplz.dto.notification;

import com.gitggal.clothesplz.entity.notification.NotificationLevel;
import java.util.UUID;

/**
 * 알림 생성 요청용 DTO
 */
public record NotificationRequest(
    UUID receiverId,

    String title,

    String content,

    NotificationLevel level
) {
}
