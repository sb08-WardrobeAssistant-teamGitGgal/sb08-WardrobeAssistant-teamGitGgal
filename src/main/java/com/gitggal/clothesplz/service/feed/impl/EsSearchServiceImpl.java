package com.gitggal.clothesplz.service.feed.impl;

import com.gitggal.clothesplz.repository.feed.FeedSearchRepository;
import com.gitggal.clothesplz.service.feed.EsSearchService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EsSearchServiceImpl implements EsSearchService {

  private final FeedSearchRepository feedSearchRepository;

  @Override
  @CircuitBreaker(name = "esSearch", fallbackMethod = "fallbackToDbLike")
  public List<UUID> searchMatchedIds(String keyword) {
    return feedSearchRepository.searchByContent(keyword).stream()
        .map(document -> UUID.fromString(document.getId()))
        .toList();
  }

  private List<UUID> fallbackToDbLike(String keyword, Exception e) {
    log.warn("[CircuitBreaker] ES 검색 실패, DB LIKE fallback 실행 - keyword: {}, cause: {}", keyword, e.getMessage());
    return null;
  }
}
