package com.gitggal.clothesplz.event.elasticsearch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;

import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.SkyStatus;
import com.gitggal.clothesplz.repository.feed.FeedSearchRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ES 동기화 재시도 핸들러 테스트")
class FeedElasticSearchRetryHandlerTest {

  @InjectMocks
  private FeedElasticSearchRetryHandler retryHandler;

  @Mock
  private FeedSearchRepository feedSearchRepository;

  @Test
  @DisplayName("ES 동기화 성공 시 feedSearchRepository.save() 호출")
  void sync_Success() {
    // given
    FeedElasticSearchSyncEvent event = new FeedElasticSearchSyncEvent(
        UUID.randomUUID(),
        "피드 내용",
        UUID.randomUUID(),
        SkyStatus.CLEAR,
        PrecipitationType.NONE,
        0L,
        Instant.now()
    );

    // when
    retryHandler.sync(event);

    // then
    then(feedSearchRepository).should().save(any());
  }

  @Test
  @DisplayName("ES 삭제 성공 시 feedSearchRepository.deleteById() 호출")
  void delete_Success() {
    // given
    UUID feedId = UUID.randomUUID();

    // when
    retryHandler.delete(feedId);

    // then
    then(feedSearchRepository).should().deleteById(feedId.toString());
  }

  @Test
  @DisplayName("ES 동기화 최종 실패 시 예외 미전파")
  void recoverSync_DoesNotThrow() {
    // given
    FeedElasticSearchSyncEvent event = new FeedElasticSearchSyncEvent(
        UUID.randomUUID(),
        "피드 내용",
        UUID.randomUUID(),
        SkyStatus.CLEAR,
        PrecipitationType.NONE,
        0L,
        Instant.now()
    );

    // when & then
    retryHandler.recoverSync(new RuntimeException("ES 연결 실패"), event);
  }

  @Test
  @DisplayName("ES 삭제 최종 실패 시 예외 미전파")
  void recoverDelete_DoesNotThrow() {
    // given
    UUID feedId = UUID.randomUUID();

    // when & then
    retryHandler.recoverDelete(new RuntimeException("ES 연결 실패"), feedId);
  }
}
