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
    if (feedSearchRepository.count() > 0) {
      log.info("[ES 초기화] 이미 인덱스에 데이터가 있어 건너뜁니다.");
      return;
    }

    log.info("[ES 초기화] 피드 데이터 인덱싱 시작");

    List<Feed> feeds = feedRepository.findAll();

    List<FeedDocument> feedDocuments = feeds.stream()
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

    feedSearchRepository.saveAll(feedDocuments);
    log.info("[ES 초기화] 피드 {}건 인덱싱 완료", feedDocuments.size());
  }
}
