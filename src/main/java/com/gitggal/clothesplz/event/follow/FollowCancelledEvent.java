package com.gitggal.clothesplz.event.follow;

import java.util.UUID;

/**
 * 팔로우 취소 이벤트
 */
public record FollowCancelledEvent(
    UUID followerId,
    UUID followeeId
) {
}
