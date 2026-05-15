package com.gitggal.clothesplz.dto.clothes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ClothesAttributeDefCreateRequest(
    @NotNull
    @NotBlank(message = "name은 공백일 수 없습니다.")
    String name,

    @NotNull
    @Size(min = 1, message = "selectableValues는 최소 1개 이상의 값이 필요합니다.")
    List<@NotBlank(message = "selectableValues의 각 값은 공백일 수 없습니다.") String> selectableValues
) {

}
