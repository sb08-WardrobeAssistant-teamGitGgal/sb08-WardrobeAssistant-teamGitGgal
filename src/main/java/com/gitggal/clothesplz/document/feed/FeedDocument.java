package com.gitggal.clothesplz.document.feed;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

@Document(indexName = "feeds")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setting(settingPath = "elasticsearch/feed-index-settings.json")
public class FeedDocument {

  @Id
  private String id;

  // 한국어 형태소 분석해서 검색
  @Field(type = FieldType.Text, analyzer = "nori_custom")
  private String content;
}
