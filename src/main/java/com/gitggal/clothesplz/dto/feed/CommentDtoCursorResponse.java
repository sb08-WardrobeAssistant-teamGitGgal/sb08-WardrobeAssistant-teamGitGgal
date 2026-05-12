package com.gitggal.clothesplz.dto.feed;

import java.util.List;
import java.util.UUID;

public record CommentDtoCursorResponse(
    List<CommentDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    long totalCount,
    String sortBy,
    String sortDirection
) {

}
