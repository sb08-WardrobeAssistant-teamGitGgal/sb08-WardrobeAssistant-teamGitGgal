package com.gitggal.clothesplz.event.feed;

import java.util.List;
import java.util.UUID;

/**
 * 팔로우 한 유저 피드 생성 알림 이벤트
 */
public record FeedCreatedEvent(
    List<UUID> followerIds,
    String authorName,
    String feedContent
) {
}
