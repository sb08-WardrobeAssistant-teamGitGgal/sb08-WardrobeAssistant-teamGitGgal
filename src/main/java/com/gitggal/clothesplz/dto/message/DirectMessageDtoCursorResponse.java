package com.gitggal.clothesplz.dto.message;

import java.util.List;
import java.util.UUID;

/**
 * DM 목록 조회 응답 DTO (커서 기반 페이지네이션)
 */
public record DirectMessageDtoCursorResponse(

    List<DirectMessageDto> data,

    String nextCursor,

    UUID nextIdAfter,

    boolean hasNext,

    long totalCount,

    String sortBy,

    String sortDirection
) {
}
