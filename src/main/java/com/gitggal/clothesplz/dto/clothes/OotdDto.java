package com.gitggal.clothesplz.dto.clothes;

import java.util.List;
import java.util.UUID;

public record OotdDto(
    UUID clothesId,
    String name,
    String imageUrl,
    String type,
    List<ClothesAttributeDefDto> attributes
) {

}
