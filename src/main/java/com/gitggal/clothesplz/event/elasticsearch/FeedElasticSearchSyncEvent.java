package com.gitggal.clothesplz.event.elasticsearch;

import java.util.UUID;

public record FeedElasticSearchSyncEvent(
    UUID feedId,
    String content
) {

}
