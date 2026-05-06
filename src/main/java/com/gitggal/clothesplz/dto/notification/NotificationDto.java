package com.gitggal.clothesplz.dto.notification;

import com.gitggal.clothesplz.entity.notification.NotificationLevel;
import java.time.Instant;
import java.util.UUID;

/**
 * SSE로 전송되는 알림 데이터 DTO
 *
 * 필드 구성
 * - id            : 이벤트 고유 ID (SSE id 필드에 사용)
 * - createdAt     : 알림 생성 시각
 * - receiverId    : 알림을 받을 사용자 ID
 * - title         : 알림 제목 (예: "새로운 의상 속성이 추가되었어요.")
 * - content       : 알림 내용 (예: "내 의상에 [사이즈] 속성을 추가해보세요.")
 * - level         : 알림 심각도 (INFO / WARNING / ERROR)
 */
public record NotificationDto(
    
    UUID id,

    Instant createdAt,

    UUID receiverId,

    String title,

    String content,

    NotificationLevel level
) {
}
