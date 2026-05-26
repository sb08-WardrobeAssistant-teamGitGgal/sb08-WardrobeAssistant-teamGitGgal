package com.gitggal.clothesplz.service.feed.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.document.feed.FeedDocument;
import com.gitggal.clothesplz.repository.feed.FeedSearchRepository;
import com.gitggal.clothesplz.service.feed.FeedSearchCacheService;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedSearchCacheServiceImpl implements FeedSearchCacheService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final ObjectMapper objectMapper;
  private final FeedSearchRepository feedSearchRepository;

  // 피드 검색 키 패턴 - feed:search:청바지
  private static final String SEARCH_KEY_PREFIX = "feed:search:";
  // TTL 5분 설정(캐시 5분 유효)
  private static final Duration SEARCH_TTL = Duration.ofMinutes(5);

  @Override
  public List<FeedDocument> searchByContent(String keyword) {
    String key = SEARCH_KEY_PREFIX + keyword;

    // redis 조회
    try {
      Object cached = redisTemplate.opsForValue().get(key);
      if (cached != null) {
        log.debug("[Cache] HIT: key={}", key);
        // 원활한 캐스팅을 위한 TypeReference로 타입 힌트 주기(FeedDocument로 역직렬화)
        return objectMapper.convertValue(cached, new TypeReference<List<FeedDocument>>() {});
      }
    } catch (Exception e) {
      log.warn("[Cache] Redis 조회 실패, fallthrough: key={}, error={}", key, e.getMessage());
    }

    // ES로 직접 조회
    List<FeedDocument> result = feedSearchRepository.searchByContent(keyword);

    // redis에 저장
    try {
      redisTemplate.opsForValue().set(key, result, SEARCH_TTL);
      log.debug("[Cache] SAVE: key={}, ttl={}m", key, SEARCH_TTL.toMinutes());
    } catch (Exception e) {
      log.warn("[Cache] Redis 저장 실패, 무시: key={}, error={}", key, e.getMessage());
    }

    return result;
  }
}
