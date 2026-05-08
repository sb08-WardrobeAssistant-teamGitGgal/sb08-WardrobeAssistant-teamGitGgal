package com.gitggal.clothesplz.dto.feed;

import com.gitggal.clothesplz.dto.user.AuthorDto;
import java.time.Instant;
import java.util.UUID;

public record CommentDto(
    UUID id,
    Instant createdAt,
    UUID feedId,
    AuthorDto author,
    String content
) {

}
