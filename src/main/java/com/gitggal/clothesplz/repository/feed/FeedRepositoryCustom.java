package com.gitggal.clothesplz.repository.feed;

import com.gitggal.clothesplz.dto.feed.FeedDto;
import com.gitggal.clothesplz.dto.feed.FeedPageRequest;
import java.util.List;

public interface FeedRepositoryCustom {

  List<FeedDto> findAllByCursor(FeedPageRequest feedPageRequest);

  long countByCondition(FeedPageRequest feedPageRequest);
}
