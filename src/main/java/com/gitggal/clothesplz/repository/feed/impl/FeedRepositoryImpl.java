package com.gitggal.clothesplz.repository.feed.impl;

import static com.gitggal.clothesplz.entity.feed.QFeed.feed;
import static com.gitggal.clothesplz.entity.profile.QProfile.profile;
import static com.gitggal.clothesplz.entity.user.QUser.user;
import static com.gitggal.clothesplz.entity.weather.QWeather.weather;

import com.gitggal.clothesplz.dto.feed.FeedCursorCondition;
import com.gitggal.clothesplz.dto.feed.FeedDto;
import com.gitggal.clothesplz.dto.feed.FeedPageRequest;
import com.gitggal.clothesplz.dto.user.AuthorDto;
import com.gitggal.clothesplz.dto.weather.PrecipitationDto;
import com.gitggal.clothesplz.dto.weather.TemperatureDto;
import com.gitggal.clothesplz.dto.weather.WeatherSummaryDto;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.SkyStatus;
import com.gitggal.clothesplz.repository.feed.FeedRepositoryCustom;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class FeedRepositoryImpl implements FeedRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<FeedDto> findAllByCursor(FeedPageRequest feedPageRequest, FeedCursorCondition feedCursorCondition) {

    return queryFactory
        .select(Projections.constructor(FeedDto.class,
            feed.id,
            feed.createdAt,
            feed.updatedAt,
            Projections.constructor(AuthorDto.class,
                user.id,
                user.name,
                profile.imageUrl
            ),
            Projections.constructor(WeatherSummaryDto.class,
                weather.id,
                weather.skyStatus,
                Projections.constructor(PrecipitationDto.class,
                    weather.precipitationType,
                    weather.precipitationAmount,
                    weather.precipitationProbability
                ),
                Projections.constructor(TemperatureDto.class,
                    weather.temperatureCurrent,
                    weather.temperatureDiff,
                    weather.temperatureMin,
                    weather.temperatureMax
                )
            ),
            feed.ootds,
            feed.content,
            feed.likeCount,
            feed.commentCount.intValue(),
            Expressions.constant(false) // 1차 매핑에서는 likedByMe 값을 false로 설정
        ))
        .from(feed)
        .join(feed.weather, weather)
        .join(feed.author, user)
        .join(profile).on(profile.user.eq(user))
        .where(
            keywordLike(feedPageRequest.keywordLike()),
            skyStatusEqual(feedPageRequest.skyStatusEqual()),
            precipitationTypeEqual(feedPageRequest.precipitationTypeEqual()),
            authorIdEqual(feedPageRequest.authorIdEqual()),
            cursorCondition(
                feedCursorCondition,
                feedPageRequest.sortBy(),
                feedPageRequest.sortDirection())
        )
        .orderBy(
            orderSpecifiers(feedPageRequest.sortBy(), feedPageRequest.sortDirection())
        )
        .limit(feedPageRequest.limit() + 1)
        .fetch();
  }

  @Override
  public long countByCondition(FeedPageRequest feedPageRequest) {
    Long count = queryFactory
        .select(feed.count())
        .from(feed)
        .join(feed.weather, weather)
        .where(
            keywordLike(feedPageRequest.keywordLike()),
            skyStatusEqual(feedPageRequest.skyStatusEqual()),
            precipitationTypeEqual(feedPageRequest.precipitationTypeEqual()),
            authorIdEqual(feedPageRequest.authorIdEqual())
        )
        .fetchOne();
    return count != null ? count : 0L;
  }

  // 키워드로 검색
  private BooleanExpression keywordLike(String keyword) {
    return StringUtils.hasText(keyword) ?
        feed.content.containsIgnoreCase(keyword) : null;
  }

  // 날씨로 검색
  private BooleanExpression skyStatusEqual(String skyStatus) {
    return StringUtils.hasText(skyStatus) ?
        weather.skyStatus.eq(SkyStatus.valueOf(skyStatus)) : null;
  }

  // 강수 유형으로 검색
  private BooleanExpression precipitationTypeEqual(String precipitationType) {
    return StringUtils.hasText(precipitationType) ?
        weather.precipitationType.eq(PrecipitationType.valueOf(precipitationType)) : null;
  }

  // 특정 피드 작성자 Id로 검색
  private BooleanExpression authorIdEqual(UUID authorId) {
    return authorId != null ? feed.author.id.eq(authorId) : null;
  }

  // 커서 조건
  private BooleanExpression cursorCondition(
      FeedCursorCondition feedCursorCondition,
      String sortBy,
      String sortDirection) {
    // 첫 페이지 요청인 경우 커서 조건 없이 처음부터 조회
    if (feedCursorCondition.idAfter() == null) {
      return null;
    }

    boolean isDesc = "DESCENDING".equals(sortDirection);

    // 좋아요로 정렬일 경우
    if ("likeCount".equals(sortBy)) {
      // 책갈피가 되는 피드의 좋아요 수
      long cursorCount = feedCursorCondition.likeCountCursor();
      return isDesc
          // 내림차순인 경우 cursor보다 좋아요가 작거나, 좋아요가 같은 경우 id가 작은 피드들 반환
          ? feed.likeCount.lt(cursorCount)
            .or(feed.likeCount.eq(cursorCount).and(feed.id.lt(feedCursorCondition.idAfter())))
          // 오름차순 경우 cursor보다 좋아요가 많거나, 좋아요가 같은 경우 id가 큰 피드들 반환
          : feed.likeCount.gt(cursorCount)
            .or(feed.likeCount.eq(cursorCount).and(feed.id.gt(feedCursorCondition.idAfter())));
    }

    // 생성 시간으로 정렬일 경우(DEFAULT)
    // 책갈피가 되는 피드의 생성 시간
    Instant cursorTime = feedCursorCondition.createdAtCursor();

    return isDesc
        ? feed.createdAt.lt(cursorTime).or(feed.createdAt.eq(cursorTime).and(feed.id.lt(feedCursorCondition.idAfter())))
        : feed.createdAt.gt(cursorTime).or(feed.createdAt.eq(cursorTime).and(feed.id.gt(feedCursorCondition.idAfter())));
  }

  // 정렬 조건
  private OrderSpecifier<?>[] orderSpecifiers(String sortBy, String sortDirection) {
    Order direction = "DESCENDING".equals(sortDirection) ? Order.DESC : Order.ASC;

    if ("likeCount".equals(sortBy)) {
      return new OrderSpecifier<?>[]{
          new OrderSpecifier<>(direction, feed.likeCount),
          new OrderSpecifier<>(direction, feed.id)
      };
    }

    return new OrderSpecifier<?>[]{
        new OrderSpecifier<>(direction, feed.createdAt),
        new OrderSpecifier<>(direction, feed.id),
    };
  }
}
