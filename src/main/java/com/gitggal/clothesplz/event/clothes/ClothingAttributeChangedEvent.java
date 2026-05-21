package com.gitggal.clothesplz.event.clothes;

import java.util.List;
import java.util.UUID;

/**
 * 의상 속성 변경 알림 이벤트
 */
public record ClothingAttributeChangedEvent(
    List<UUID> allUserIds,
    String attributeName,
    ChangeType changeType
) {

  public enum ChangeType {ADDED, UPDATED, DELETED}
}
