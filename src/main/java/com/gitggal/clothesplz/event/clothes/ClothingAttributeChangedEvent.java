package com.gitggal.clothesplz.event.clothes;

import java.util.UUID;

public record ClothingAttributeChangedEvent(
    UUID userId,
    String attributeName,
    ChangeType changeType
) {

  public enum ChangeType {ADDED, UPDATED, DELETED}
}
