package com.gitggal.clothesplz.event.elasticsearch;

import com.gitggal.clothesplz.document.feed.FeedDocument;
import com.gitggal.clothesplz.repository.feed.FeedSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Slf4j
@Component
@RequiredArgsConstructor
public class FeedElasticSearchEventListener {

  private final FeedSearchRepository feedSearchRepository;

  @Async("taskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleFeedSync(FeedElasticSearchSyncEvent event) {
    try {
      feedSearchRepository.save(FeedDocument.builder()
          .id(event.feedId().toString())
          .content(event.content())
          .authorId(event.authorId().toString())
          .skyStatus(event.skyStatus().name())
          .precipitationType(event.precipitationType().name())
          .likeCount(event.likeCount())
          .createdAt(event.createdAt())
          .build());
    } catch (Exception e) {
      log.error("[ES 동기화 실패] feedId={}, authorId={}, operation=SYNC, error={}",
          event.feedId(), event.authorId(), e.getMessage(), e);
      // TODO: Kafka 도입 시 재시도/복구 로직 구현 예정
    }
  }

  @Async("taskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleFeedDelete(FeedElasticSearchDeleteEvent event) {
    try {
      feedSearchRepository.deleteById(event.feedId().toString());
    } catch (Exception e) {
      log.error("[ES 삭제 실패] feedId={}, operation=DELETE, error={}",
          event.feedId(), e.getMessage(), e);
      // TODO: Kafka 도입 시 재시도/복구 로직 구현 예정
    }
  }
}
