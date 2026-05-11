package com.gitggal.clothesplz.dto.clothes;

import com.gitggal.clothesplz.entity.clothes.ClothesType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record ClothesCreateRequest(
    @NotNull(message = "소유자 ID는 필수입니다.")
    UUID ownerId,

    @NotBlank(message = "의상 이름은 공백일 수 없습니다.")
    @Size(max = 100, message = "의상 이름은 100자 이하여야 합니다.")
    String name,

    @NotNull(message = "의상 타입은 필수입니다.")
    ClothesType type,

    @Valid
    @NotNull(message = "속성 목록은 필수입니다.")
    List<ClothesAttributeDto> attributes
) {

}
