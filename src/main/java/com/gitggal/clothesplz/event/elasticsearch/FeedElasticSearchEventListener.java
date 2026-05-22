package com.gitggal.clothesplz.event.elasticsearch;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class FeedElasticSearchEventListener {

  private final FeedElasticSearchRetryHandler searchRetryHandler;

  @Async("taskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleFeedSync(FeedElasticSearchSyncEvent event) {
    searchRetryHandler.sync(event);
  }

  @Async("taskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleFeedDelete(FeedElasticSearchDeleteEvent event) {
    searchRetryHandler.delete(event.feedId());
  }
}
