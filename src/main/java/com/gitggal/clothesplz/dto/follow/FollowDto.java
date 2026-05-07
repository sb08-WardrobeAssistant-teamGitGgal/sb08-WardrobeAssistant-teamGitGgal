package com.gitggal.clothesplz.dto.follow;

import java.util.UUID;

/**
 * 팔로우 생성/조회 응답 DTO
 */
public record FollowDto(
    UUID id,

    UserSummary follower,

    UserSummary followee
) {
}
