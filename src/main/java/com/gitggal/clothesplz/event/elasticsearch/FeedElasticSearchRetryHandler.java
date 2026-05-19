package com.gitggal.clothesplz.event.elasticsearch;

import com.gitggal.clothesplz.document.feed.FeedDocument;
import com.gitggal.clothesplz.repository.feed.FeedSearchRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedElasticSearchRetryHandler {

  private final FeedSearchRepository feedSearchRepository;

  // 재시도 최대 3회, 재시도 간격을 이전 대기 시간 * 2배씩 증가하며 시도
  @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
  public void sync(FeedElasticSearchSyncEvent event) {
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

  @Recover
  public void recoverSync(Exception e, FeedElasticSearchSyncEvent event) {
    log.error("[ES 동기화 최종 실패] feedId={}, authorId={}, operation=SYNC, error={}",
        event.feedId(), event.authorId(), e.getMessage(), e);
  }

  @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
  public void delete(UUID feedId) {
    feedSearchRepository.deleteById(feedId.toString());
  }

  @Recover
  public void recoverDelete(Exception e, UUID feedId) {
    log.error("[ES 삭제 최종 실패] feedId={}, operation=DELETE, error={}", feedId, e.getMessage(), e);
  }
}
