package com.gitggal.clothesplz.entity.notification;

/**
 * 알림의 심각도를 나타내는 enum
 *
 * 필드 구성
 * - INFO          : 일반 정보성의 알림 (예: 좋아요, 팔로우, DM 수신, 피드 등록)
 * - WARNING       : 주의가 필요한 알림 (예: 권한 변경)
 * - ERROR         : 오류 관련 알림 (시스템 오류로 인한 처리 실패 등)
 */
public enum NotificationLevel {

    INFO,

    WARNING,

    ERROR
}
