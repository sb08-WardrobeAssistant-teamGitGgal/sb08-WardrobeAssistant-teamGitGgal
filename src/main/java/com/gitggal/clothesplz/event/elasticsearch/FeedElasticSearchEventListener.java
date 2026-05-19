package com.gitggal.clothesplz.event.elasticsearch;

import com.gitggal.clothesplz.document.feed.FeedDocument;
import com.gitggal.clothesplz.repository.feed.FeedSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class FeedElasticSearchEventListener {

  private final FeedSearchRepository feedSearchRepository;

  @Async("taskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleFeedSync(FeedElasticSearchSyncEvent event) {
    feedSearchRepository.save(FeedDocument.builder()
        .id(event.feedId().toString())
        .content(event.content())
        .authorId(event.authorId().toString())
        .skyStatus(event.skyStatus().name())
        .precipitationType(event.precipitationType().name())
        .likeCount(event.likeCount())
        .createdAt(event.createdAt())
        .build());
  }

  @Async("taskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleFeedDelete(FeedElasticSearchDeleteEvent event) {
    feedSearchRepository.deleteById(event.feedId().toString());
  }

}
