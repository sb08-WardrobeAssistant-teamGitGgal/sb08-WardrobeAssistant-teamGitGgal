package com.gitggal.clothesplz.init;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.gitggal.clothesplz.entity.feed.Feed;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.SkyStatus;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.repository.feed.FeedRepository;
import com.gitggal.clothesplz.repository.feed.FeedSearchRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedElasticSearchInitializer 테스트")
class FeedElasticSearchInitializerTest {

  @Mock
  private FeedRepository feedRepository;

  @Mock
  private FeedSearchRepository feedSearchRepository;

  @InjectMocks
  private FeedElasticSearchInitializer initializer;

  private Feed mockFeed() {
    Feed feed = mock(Feed.class);
    User author = mock(User.class);
    Weather weather = mock(Weather.class);

    given(feed.getId()).willReturn(UUID.randomUUID());
    given(feed.getContent()).willReturn("테스트 피드 내용");
    given(feed.getAuthor()).willReturn(author);
    given(feed.getWeather()).willReturn(weather);
    given(feed.getLikeCount()).willReturn(0L);
    given(feed.getCreatedAt()).willReturn(Instant.now());
    given(author.getId()).willReturn(UUID.randomUUID());
    given(weather.getSkyStatus()).willReturn(SkyStatus.CLEAR);
    given(weather.getPrecipitationType()).willReturn(PrecipitationType.NONE);

    return feed;
  }

  @Nested
  @DisplayName("ES 인덱싱 스킵 관련 테스트")
  class SkipTests {

    @Test
    @DisplayName("DB와 ES 건수가 일치하면 인덱싱 스킵하는 경우")
    void run_SkipsIndexing_WhenCountsMatch() throws Exception {
      // given
      given(feedRepository.count()).willReturn(7L);
      given(feedSearchRepository.count()).willReturn(7L);

      // when
      initializer.run();

      // then
      then(feedSearchRepository).should(never()).deleteAll();
      then(feedRepository).should(never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("DB와 ES 모두 비어있어 인덱싱 스킵하는 경우")
    void run_SkipsIndexing_WhenBothEmpty() throws Exception {
      // given
      given(feedRepository.count()).willReturn(0L);
      given(feedSearchRepository.count()).willReturn(0L);

      // when
      initializer.run();

      // then
      then(feedSearchRepository).should(never()).deleteAll();
      then(feedRepository).should(never()).findAll(any(Pageable.class));
    }
  }

  @Nested
  @DisplayName("ES 인덱싱 실행 관련 테스트")
  class ReindexTests {

    @Test
    @DisplayName("ES가 비어있고 DB에 데이터가 있으면 전체 인덱싱")
    void run_IndexesAll_WhenEsIsEmpty() throws Exception {
      // given
      Feed feed = mockFeed();
      given(feedRepository.count()).willReturn(1L);
      given(feedSearchRepository.count()).willReturn(0L);
      given(feedRepository.findAll(any(PageRequest.class)))
          .willReturn(new PageImpl<>(List.of(feed), PageRequest.of(0, 100), 1));

      // when
      initializer.run();

      // then
      then(feedSearchRepository).should().deleteAll();
      then(feedSearchRepository).should().saveAll(anyList());
    }

    @Test
    @DisplayName("부분 인덱싱 상태이면 전체 삭제 후 재인덱싱")
    void run_ReindexesAll_WhenPartiallyIndexed() throws Exception {
      // given
      Feed feed = mockFeed();
      given(feedRepository.count()).willReturn(7L);
      given(feedSearchRepository.count()).willReturn(3L);
      given(feedRepository.findAll(any(PageRequest.class)))
          .willReturn(new PageImpl<>(List.of(feed), PageRequest.of(0, 100), 7));

      // when
      initializer.run();

      // then
      then(feedSearchRepository).should().deleteAll();
      then(feedSearchRepository).should().saveAll(anyList());
    }

    @Test
    @DisplayName("피드가 배치 크기를 초과하면 페이지 단위로 나누어 인덱싱")
    void run_IndexesInBatches_WhenFeedsExceedBatchSize() throws Exception {
      // given
      Feed feed1 = mockFeed();
      Feed feed2 = mockFeed();

      given(feedRepository.count()).willReturn(150L);
      given(feedSearchRepository.count()).willReturn(0L);
      given(feedRepository.findAll(any(PageRequest.class)))
          .willReturn(new PageImpl<>(List.of(feed1), PageRequest.of(0, 100), 150))
          .willReturn(new PageImpl<>(List.of(feed2), PageRequest.of(1, 100), 150));

      // when
      initializer.run();

      // then
      then(feedSearchRepository).should().deleteAll();
      then(feedSearchRepository).should(times(2)).saveAll(anyList());
    }
  }
}
