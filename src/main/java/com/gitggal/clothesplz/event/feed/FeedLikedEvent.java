package com.gitggal.clothesplz.event.feed;

import java.util.UUID;

/**
 * 피드 좋아요 알림 이벤트
 */
public record FeedLikedEvent(
    UUID feedOwnerId,
    String likerName,
    String feedContent
) {
}
