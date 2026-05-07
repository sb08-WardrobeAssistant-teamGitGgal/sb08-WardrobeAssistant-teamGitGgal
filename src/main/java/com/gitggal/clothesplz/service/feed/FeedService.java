package com.gitggal.clothesplz.service.feed;

import com.gitggal.clothesplz.dto.feed.FeedCreateRequest;
import com.gitggal.clothesplz.dto.feed.FeedDto;

public interface FeedService {

  // 피드 등록
  FeedDto createFeed(FeedCreateRequest feedCreateRequest);
}
