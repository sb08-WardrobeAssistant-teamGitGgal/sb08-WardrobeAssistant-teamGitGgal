package com.gitggal.clothesplz.event.feed;

import java.util.UUID;

/**
 * 피드 댓글 생성 알림 이벤트
 */
public record FeedCommentCreatedEvent(
    UUID feedOwnerId,
    String commenterName,
    String commentContent
) {
}
