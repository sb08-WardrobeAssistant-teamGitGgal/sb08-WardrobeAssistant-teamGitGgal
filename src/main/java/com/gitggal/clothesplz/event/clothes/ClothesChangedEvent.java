package com.gitggal.clothesplz.event.clothes;

import java.util.UUID;

public record ClothesChangedEvent(
    UUID userId,
    String clothesName,
    ChangeType changeType
) {

  public enum ChangeType {
    CREATED,
    UPDATED,
    DELETED
  }
}
