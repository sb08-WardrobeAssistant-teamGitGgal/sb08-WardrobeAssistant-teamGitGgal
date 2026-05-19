package com.gitggal.clothesplz.dto.feed;

import java.time.Instant;
import java.util.UUID;

public record FeedCursorCondition(
    Instant createdAtCursor,
    Long likeCountCursor,
    UUID idAfter
) {

}
