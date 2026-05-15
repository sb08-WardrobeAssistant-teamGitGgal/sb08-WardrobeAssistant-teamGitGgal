package com.gitggal.clothesplz.dto.user;

import com.gitggal.clothesplz.entity.user.UserRole;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest(

    @NotNull(message = "역할은 필수입니다")
    UserRole role
) {

}
