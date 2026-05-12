package com.gitggal.clothesplz.dto.clothes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ClothesAttributeDto(
    @NotNull(message = "속성 정의 ID는 필수입니다.")
    UUID definitionId,

    @NotBlank(message = "속성 값은 공백일 수 없습니다.")
    String value
) {
}
