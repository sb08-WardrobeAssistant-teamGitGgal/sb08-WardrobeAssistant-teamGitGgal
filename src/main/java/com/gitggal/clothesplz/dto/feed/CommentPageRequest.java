package com.gitggal.clothesplz.dto.feed;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CommentPageRequest(
    Instant cursor,

    UUID idAfter,

    @NotNull(message = "limit은 필수입니다.")
    @Min(value = 1, message = "limit은 1 이상이어야 합니다.")
    Integer limit
) {

}
