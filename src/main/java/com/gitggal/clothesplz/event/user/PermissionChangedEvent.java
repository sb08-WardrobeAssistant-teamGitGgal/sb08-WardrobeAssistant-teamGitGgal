package com.gitggal.clothesplz.event.user;

import com.gitggal.clothesplz.entity.user.UserRole;
import java.util.UUID;

/**
 * 권한 변경 알림 이벤트
 */
public record PermissionChangedEvent(
    UUID userId,
    UserRole newRole
) {
}
