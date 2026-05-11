package com.gitggal.clothesplz.dto.clothes;

import com.gitggal.clothesplz.entity.clothes.ClothesType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ClothesUpdateRequest(
    @Size(min = 1, max = 100, message = "의상 이름은 100자 이하여야 합니다.")
    String name,

    ClothesType type,

    List<@NotNull @Valid ClothesAttributeDto> attributes
) {

}
