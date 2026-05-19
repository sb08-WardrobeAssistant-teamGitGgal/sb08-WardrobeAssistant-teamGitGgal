package com.gitggal.clothesplz.repository.feed;

import com.gitggal.clothesplz.document.feed.FeedDocument;
import java.util.List;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface FeedSearchRepository extends ElasticsearchRepository<FeedDocument, String> {

  // {"match": {"content": {"query": "?0"}}}
  // 형태소 분석 후 content 필드명으로 검색
  @Query("{\"match\": {\"content\": {\"query\": \"?0\"}}}")
  List<FeedDocument> searchByContent(String keyword);
}
