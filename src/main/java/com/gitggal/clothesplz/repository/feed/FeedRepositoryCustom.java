package com.gitggal.clothesplz.repository.feed;

import com.gitggal.clothesplz.dto.feed.FeedCursorCondition;
import com.gitggal.clothesplz.dto.feed.FeedDto;
import com.gitggal.clothesplz.dto.feed.FeedPageRequest;
import java.util.List;
import java.util.UUID;

public interface FeedRepositoryCustom {

  List<FeedDto> findAllByCursor(FeedPageRequest feedPageRequest, FeedCursorCondition feedCursorCondition, List<UUID> esMatchedIds);

  long countByCondition(FeedPageRequest feedPageRequest, List<UUID> esMatchedIds);
}
