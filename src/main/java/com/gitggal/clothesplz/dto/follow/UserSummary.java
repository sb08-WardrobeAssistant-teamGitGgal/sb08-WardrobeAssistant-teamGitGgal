package com.gitggal.clothesplz.dto.follow;

import java.util.UUID;

/**
 * 사용자 요약 정보
 */
public record UserSummary(
    UUID userId,

    String name,

    String profileImageUrl
) {
}
