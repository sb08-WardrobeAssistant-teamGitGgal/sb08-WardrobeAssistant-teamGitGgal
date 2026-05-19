package com.gitggal.clothesplz.dto.clothes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ClothesAttributeDefCreateRequest(
    @NotBlank(message = "name은 공백일 수 없습니다.")
    @Size(min = 1, max = 20, message = "name은 2자 이상 20자 이하로 입력해주세요.")
    String name,

    @NotNull
    @Size(min = 1, message = "selectableValues는 최소 1개 이상의 값이 필요합니다.")
    List<@NotBlank(message = "selectableValues의 각 값은 공백일 수 없습니다.") String> selectableValues
) {

}
