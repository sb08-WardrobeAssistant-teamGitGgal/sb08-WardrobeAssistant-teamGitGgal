package com.gitggal.clothesplz.dto.feed;

import com.gitggal.clothesplz.dto.clothes.OotdDto;
import com.gitggal.clothesplz.dto.user.AuthorDto;
import com.gitggal.clothesplz.dto.weather.WeatherSummaryDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FeedDto(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    AuthorDto author,
    WeatherSummaryDto weather,
    List<OotdDto> ootds,
    String content,
    long likeCount,
    int commentCount,
    boolean likedByMe
) {
  public FeedDto withLikedByMe(boolean likedByMe) {
    return new FeedDto(id, createdAt, updatedAt, author, weather,
        ootds, content, likeCount, commentCount, likedByMe);
  }
}
