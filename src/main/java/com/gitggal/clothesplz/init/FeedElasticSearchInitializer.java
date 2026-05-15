package com.gitggal.clothesplz.init;

import com.gitggal.clothesplz.document.feed.FeedDocument;
import com.gitggal.clothesplz.entity.feed.Feed;
import com.gitggal.clothesplz.repository.feed.FeedRepository;
import com.gitggal.clothesplz.repository.feed.FeedSearchRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 처음 es를 도커로 실행할 때 기존 데이터를 es에 한 번에 저장하기 위한 컴포넌트
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class FeedElasticSearchInitializer implements CommandLineRunner {

  private final FeedRepository feedRepository;
  private final FeedSearchRepository feedSearchRepository;

  @Override
  @Transactional(readOnly = true)
  public void run(String... args) throws Exception {
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
      Page<Feed> feedPage = feedRepository.findAll(PageRequest.of(page, BATCH_SIZE));
      if (feedPage.isEmpty()) break;

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

      if (!feedPage.hasNext()) break;
      page++;
    }
    log.info("[ES 초기화] 피드 {}건 인덱싱 완료", totalIndexed);
  }
}
