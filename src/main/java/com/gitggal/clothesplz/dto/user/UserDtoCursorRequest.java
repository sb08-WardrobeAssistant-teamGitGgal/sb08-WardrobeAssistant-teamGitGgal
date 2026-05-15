package com.gitggal.clothesplz.dto.user;

import com.gitggal.clothesplz.entity.user.UserRole;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record UserDtoCursorRequest(
    String cursor,
    UUID idAfter,

    @NotNull(message = "limit은 필수입니다.")
    @Min(value = 1, message = "limit은 1 이상이어야 합니다.")
    Integer limit,

    @NotBlank(message = "정렬 기준은 필수입니다.")
    @Pattern(regexp = "^(email|createdAt)$")
    String sortBy,

    @NotBlank(message = "정렬 방향은 필수입니다.")
    @Pattern(regexp = "^(ASCENDING|DESCENDING)$")
    String sortDirection,
    String emailLike,
    UserRole roleEqual,
    Boolean locked
) {

    @AssertTrue(message = "cursor와 idAfter는 함께 전달되거나 비워야 합니다.")
    public boolean isCursorPairValid(){
        return (cursor == null) == (idAfter == null);
    }
}
