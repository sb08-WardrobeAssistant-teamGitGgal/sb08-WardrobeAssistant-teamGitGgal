package com.gitggal.clothesplz.dto.follow;

import java.util.List;
import java.util.UUID;

/**
 * 팔로워/팔로잉 목록 응답 DTO (커서 기반 페이지네이션)
 */
public record FollowListResponse(
    List<FollowDto> data,

    String nextCursor,

    UUID nextIdAfter,

    boolean hasNext,

    long totalCount,

    String sortBy,

    String sortDirection
) {
}
