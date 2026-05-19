package com.gitggal.clothesplz.dto.clothes;

import com.gitggal.clothesplz.entity.clothes.ClothesType;
import java.util.List;
import java.util.UUID;

public record ClothesDto(
    UUID id,
    UUID ownerId,
    String name,
    String imageUrl,
    ClothesType type,
    List<ClothesAttributeWithDefDto> attributes
) {

}
