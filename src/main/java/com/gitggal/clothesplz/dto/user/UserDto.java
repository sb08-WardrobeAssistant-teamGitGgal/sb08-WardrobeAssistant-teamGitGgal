package com.gitggal.clothesplz.dto.user;

import com.gitggal.clothesplz.entity.user.UserRole;
import java.time.Instant;
import java.util.UUID;

public record UserDto(
    UUID id,
    Instant createdAt,
    String email,
    String name,
    UserRole role,
    boolean locked
) {

}
