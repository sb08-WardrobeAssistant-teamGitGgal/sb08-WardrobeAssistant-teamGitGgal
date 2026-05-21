package com.gitggal.clothesplz.event.elasticsearch;

import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.SkyStatus;
import java.time.Instant;
import java.util.UUID;

public record FeedElasticSearchSyncEvent(
    UUID feedId,
    String content,
    UUID authorId,
    SkyStatus skyStatus,
    PrecipitationType precipitationType,
    long likeCount,
    Instant createdAt
) {

}
