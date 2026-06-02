package com.gitggal.clothesplz.init;

import com.gitggal.clothesplz.document.feed.FeedDocument;
import com.gitggal.clothesplz.entity.feed.Feed;
import com.gitggal.clothesplz.repository.feed.FeedRepository;
import com.gitggal.clothesplz.repository.feed.FeedSearchRepository;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 처음 es를 도커로 실행할 때 기존 데이터를 es에 한 번에 저장하기 위한 컴포넌트
// 다중 서버를 위한 redis 분산 락을 적용하여 서버 1대만 ES 인덱싱 작업 실행하도록 하기 위함
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class FeedElasticSearchInitializer implements CommandLineRunner {

  private static final String LOCK_KEY = "feed:es:init:lock";
  private static final Duration LOCK_TTL = Duration.ofMinutes(30);

  private final FeedRepository feedRepository;
  private final FeedSearchRepository feedSearchRepository;
  private final RedisTemplate<String, Object> redisTemplate;

  @Override
  @Transactional(readOnly = true)
  public void run(String... args) throws Exception {
    String lockValue = UUID.randomUUID().toString();

    // 먼저 도착한 서버가 락 획득 -> 뒤늦게 온 서버들 락 획득 실패
    Boolean acquired = redisTemplate.opsForValue()
        .setIfAbsent(LOCK_KEY, lockValue, LOCK_TTL);

    if (!Boolean.TRUE.equals(acquired)) {
      log.info("[ES 초기화] 다른 서버에서 초기화 진행 중...., 건너뜁니다.");
      return;
    }

    try {
      long dbCount = feedRepository.count();
      long esCount = feedSearchRepository.count();

      if (dbCount == esCount) {
        log.info("[ES 초기화] 정합성 일치 ({}건), 건너뜁니다.", dbCount);
        return;
      }

      log.info("[ES 초기화] 정합성 불일치 (DB: {}건, ES: {}건) - 전체 재인덱싱 시작", dbCount, esCount);
      feedSearchRepository.deleteAll();

      log.info("[ES 초기화] 피드 데이터 인덱싱 시작");

      int page = 0;
      final int BATCH_SIZE = 100;
      long totalIndexed = 0;

      while (true) {
        Page<Feed> feedPage = feedRepository.findAll(
            PageRequest.of(page, BATCH_SIZE, Sort.by(Sort.Direction.ASC, "id")));
        if (feedPage.isEmpty())
          break;

        List<FeedDocument> documents = feedPage.getContent().stream()
            .map(feed -> FeedDocument.builder()
                .id(feed.getId().toString())
                .content(feed.getContent())
                .authorId(feed.getAuthor().getId().toString())
                .skyStatus(feed.getWeather().getSkyStatus().name())
                .precipitationType(feed.getWeather().getPrecipitationType().name())
                .likeCount(feed.getLikeCount())
                .createdAt(feed.getCreatedAt())
                .build())
            .toList();

        feedSearchRepository.saveAll(documents);
        totalIndexed += documents.size();

        if (!feedPage.hasNext())
          break;
        page++;
      }
      log.info("[ES 초기화] 피드 {}건 인덱싱 완료", totalIndexed);
    } finally { // 서버가 인덱싱에 성공하든 실패하든 락 해제 실행
      // 여러 명령들을 원자적으로 실행하기 위해 루아 스크립트 사용
      String luaScript =
          "if redis.call('get', KEYS[1]) == ARGV[1] " + // 락 획득한 서버의 uuid와 Redis에 저장된 키 비교
              "then return redis.call('del', KEYS[1]) " + // 일치하면 키 삭제 및 락 해제
              "else return 0 end"; // 일치하지 않으면 아무것도 안 함

      redisTemplate.execute(
          new DefaultRedisScript<>(luaScript, Long.class), // 실행할 스크립트
          List.of(LOCK_KEY), // KEYS[1] 에 들어갈 값
          lockValue // ARGV[1]에 들어갈 값
      );
    }
  }
}
