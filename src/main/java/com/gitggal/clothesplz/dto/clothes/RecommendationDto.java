package com.gitggal.clothesplz.dto.clothes;

import java.util.List;

public record RecommendationDto(
    String weatherId,
    String userId,
    List<ClothesDto> clothes
) {

}
