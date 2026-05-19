package com.gitggal.clothesplz.event.feed;

import com.gitggal.clothesplz.dto.notification.NotificationRequest;
import com.gitggal.clothesplz.entity.notification.NotificationLevel;
import com.gitggal.clothesplz.service.notification.NotificationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class FeedNotificationEventListener {

  private final NotificationService notificationService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCommentCreated(FeedCommentCreatedEvent event) {
    notificationService.send(new NotificationRequest(
        event.feedOwnerId(),
        event.commenterName() + "님이 댓글을 달았어요.",
        event.commentContent(),
        NotificationLevel.INFO
    ));
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleFeedLiked(FeedLikedEvent event) {
    notificationService.send(new NotificationRequest(
        event.feedOwnerId(),
        event.likerName() + "님이 내 피드를 좋아합니다.",
        event.feedContent(),
        NotificationLevel.INFO
    ));
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleFeedCreated(FeedCreatedEvent event) {
    for (UUID followerId : event.followerIds()) {
      notificationService.send(new NotificationRequest(
          followerId,
          event.authorName() + "님이 새로운 피드를 작성했어요.",
          event.feedContent(),
          NotificationLevel.INFO
      ));
    }
  }
}
