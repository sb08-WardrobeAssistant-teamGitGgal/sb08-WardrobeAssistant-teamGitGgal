package com.gitggal.clothesplz.service.feed;

import com.gitggal.clothesplz.document.feed.FeedDocument;
import java.util.List;

public interface FeedSearchCacheService {

  List<FeedDocument> searchByContent(String keyword);

}
