package com.gitggal.clothesplz.dto.clothes;

import com.gitggal.clothesplz.entity.clothes.ClothesType;
import java.util.List;
import java.util.UUID;

public record OotdDto(
    UUID clothesId,
    String name,
    String imageUrl,
    ClothesType type,
    List<ClothesAttributeDefDto> attributes
) {

}
