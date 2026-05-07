package com.gitggal.clothesplz.dto.user;

import java.util.UUID;

public record AuthorDto(
    UUID userId,
    String name,
    String profileImageUrl
) {

}
