package com.gitggal.clothesplz.service.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.document.feed.FeedDocument;
import com.gitggal.clothesplz.repository.feed.FeedSearchRepository;
import com.gitggal.clothesplz.service.feed.impl.FeedSearchCacheServiceImpl;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("피드 검색 캐시 서비스 테스트")
class FeedSearchCacheServiceTest {

    @InjectMocks
    private FeedSearchCacheServiceImpl feedSearchCacheService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private FeedSearchRepository feedSearchRepository;

    private static final String KEYWORD = "청바지";
    private static final String CACHE_KEY = "feed:search:청바지";

    @Nested
    @DisplayName("캐시 HIT")
    class CacheHit {

        @BeforeEach
        void setUp() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
        }

        @Test
        @DisplayName("Redis에 값이 있으면 ES 조회 없이 캐시 결과 반환")
        void searchByContent_CacheHit_ReturnsCachedResult() {
            // given
            FeedDocument mockDoc = FeedDocument.builder().id("feed-1").content("청바지 코디").build();
            given(valueOps.get(CACHE_KEY)).willReturn(new Object());
            given(objectMapper.convertValue(any(), any(TypeReference.class))).willReturn(List.of(mockDoc));

            // when
            List<FeedDocument> result = feedSearchCacheService.searchByContent(KEYWORD);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo("feed-1");
            then(feedSearchRepository).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("캐시 MISS")
    class CacheMiss {

        @BeforeEach
        void setUp() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
        }

        @Test
        @DisplayName("Redis에 값이 없으면 ES 조회 후 Redis에 저장하고 반환")
        void searchByContent_CacheMiss_QueriesESAndSavesToCache() {
            // given
            FeedDocument mockDoc = FeedDocument.builder().id("feed-1").content("청바지 코디").build();
            given(valueOps.get(CACHE_KEY)).willReturn(null);
            given(feedSearchRepository.searchByContent(KEYWORD)).willReturn(List.of(mockDoc));

            // when
            List<FeedDocument> result = feedSearchCacheService.searchByContent(KEYWORD);

            // then
            assertThat(result).hasSize(1);
            then(feedSearchRepository).should().searchByContent(KEYWORD);
            then(valueOps).should().set(eq(CACHE_KEY), eq(List.of(mockDoc)), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("Redis 장애")
    class RedisFailure {

        @BeforeEach
        void setUp() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
        }

        @Test
        @DisplayName("Redis 조회 실패 시 ES 직접 조회 (fallthrough)")
        void searchByContent_RedisGetFails_FallsBackToES() {
            // given
            FeedDocument mockDoc = FeedDocument.builder().id("feed-1").content("청바지 코디").build();
            given(valueOps.get(CACHE_KEY)).willThrow(new RuntimeException("Redis down"));
            given(feedSearchRepository.searchByContent(KEYWORD)).willReturn(List.of(mockDoc));

            // when
            List<FeedDocument> result = feedSearchCacheService.searchByContent(KEYWORD);

            // then
            assertThat(result).hasSize(1);
            then(feedSearchRepository).should().searchByContent(KEYWORD);
        }

        @Test
        @DisplayName("Redis 저장 실패해도 ES 조회 결과 정상 반환")
        void searchByContent_RedisSetFails_StillReturnsResult() {
            // given
            given(valueOps.get(CACHE_KEY)).willReturn(null);
            given(feedSearchRepository.searchByContent(KEYWORD)).willReturn(List.of(mock(FeedDocument.class)));
            doThrow(new RuntimeException("Redis down")).when(valueOps).set(any(), any(), any(Duration.class));

            // when & then
            assertThatCode(() -> feedSearchCacheService.searchByContent(KEYWORD))
                .doesNotThrowAnyException();
        }
    }
}
