package com.gitggal.clothesplz.service.feed;

import com.gitggal.clothesplz.dto.feed.FeedCreateRequest;
import com.gitggal.clothesplz.dto.feed.FeedDto;
import com.gitggal.clothesplz.dto.feed.FeedUpdateRequest;
import java.util.UUID;

public interface FeedService {

  // 피드 등록
  FeedDto createFeed(FeedCreateRequest feedCreateRequest);

  // 피드 수정
  FeedDto updateFeed(UUID feedId, FeedUpdateRequest feedUpdateRequest);
}
