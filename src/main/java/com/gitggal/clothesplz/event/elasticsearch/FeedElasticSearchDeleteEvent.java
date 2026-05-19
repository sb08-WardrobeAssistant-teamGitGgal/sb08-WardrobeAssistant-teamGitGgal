package com.gitggal.clothesplz.event.elasticsearch;

import java.util.UUID;

public record FeedElasticSearchDeleteEvent(
    UUID feedId
) {

}
