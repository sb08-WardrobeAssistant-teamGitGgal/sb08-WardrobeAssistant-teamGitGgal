package com.gitggal.clothesplz.dto.clothes;

import java.util.List;
import java.util.UUID;

public record ClothesAttributeDefDto(
    UUID definitionId,
    String definitionName,
    List<String> selectableValues,
    String value
) {
}
