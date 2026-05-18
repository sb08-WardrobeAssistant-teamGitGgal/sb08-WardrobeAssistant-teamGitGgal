package com.gitggal.clothesplz.dto.clothes;

import java.util.List;
import java.util.UUID;

public record ClothesAttributeDefDto(
    UUID id,
    String name,
    List<String> selectableValues
) {
}
