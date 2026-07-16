package com.gitggal.clothesplz.event.follow;

import java.util.UUID;

/**
 * 팔로우 생성 알림 이벤트
 */
public record FollowCreatedEvent(
    UUID followerId,
    UUID followeeId,
    String followerName
) {
}
