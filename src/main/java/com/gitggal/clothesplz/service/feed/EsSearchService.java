package com.gitggal.clothesplz.service.feed;

import java.util.List;
import java.util.UUID;

public interface EsSearchService {
  List<UUID> searchMatchedIds(String keyword);
}
