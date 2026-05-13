package com.gitggal.clothesplz.repository.feed;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitggal.clothesplz.config.QuerydslConfig;
import com.gitggal.clothesplz.entity.feed.Feed;
import com.gitggal.clothesplz.entity.feed.FeedLike;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.weather.Location;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.SkyStatus;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.entity.weather.WindPhrase;
import com.gitggal.clothesplz.repository.RepositoryTestSupport;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Import(QuerydslConfig.class)
@EnableJpaAuditing
@AutoConfigureTestDatabase(replace = Replace.NONE)
@DisplayName("피드 좋아요 레포지토리 테스트")
public class FeedLikeRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private FeedLikeRepository feedLikeRepository;

  private UUID userId;
  private UUID weatherId;

  @BeforeEach
  void setUp() {
    Location location = em.persistAndFlush(Location.builder()
        .latitude(37.5)
        .longitude(127.0)
        .gridX(60)
        .gridY(127)
        .locationNames("서울")
        .build());

    Weather weather = em.persistAndFlush(Weather.builder()
        .forecastedAt(OffsetDateTime.now())
        .forecastAt(OffsetDateTime.now())
        .location(location)
        .skyStatus(SkyStatus.CLEAR)
        .precipitationType(PrecipitationType.NONE)
        .precipitationAmount(0.0)
        .precipitationProbability(0.0)
        .humidity(50.0)
        .humidityDiff(0.0)
        .temperatureCurrent(20.0)
        .temperatureDiff(0.0)
        .temperatureMin(15.0)
        .temperatureMax(25.0)
        .windSpeed(2.0)
        .windPhrase(WindPhrase.WEAK)
        .build());

    weatherId = weather.getId();
    userId = em.persistAndFlush(new User("테스트유저", "user@test.com", "password")).getId();

    em.clear();
  }

  private Feed saveFeed(User author) {
    Weather managedWeather = em.find(Weather.class, weatherId);
    return em.persistAndFlush(new Feed(managedWeather, author, List.of(), "피드 내용"));
  }

  @Test
  @DisplayName("좋아요를 누른 피드 ID만 반환한다")
  void findFeedIdsByUserId_returnsOnlyLikedFeedIds() {
    User user = em.find(User.class, userId);
    Feed likedFeed1 = saveFeed(user);
    Feed likedFeed2 = saveFeed(user);
    Feed notLikedFeed = saveFeed(user);

    em.persistAndFlush(new FeedLike(likedFeed1, user));
    em.persistAndFlush(new FeedLike(likedFeed2, user));
    em.clear();

    List<UUID> feedIds = List.of(likedFeed1.getId(), likedFeed2.getId(), notLikedFeed.getId());
    Set<UUID> result = feedLikeRepository.findFeedIdsByUserId(userId, feedIds);

    // 피드 1,2만 포함
    assertThat(result).containsExactlyInAnyOrder(likedFeed1.getId(), likedFeed2.getId());
  }

  @Test
  @DisplayName("다른 유저의 좋아요는 포함되지 않는다")
  void findFeedIdsByUserId_otherUsersLike_notIncluded() {
    User user = em.find(User.class, userId);
    User otherUser = em.persistAndFlush(new User("다른유저", "other@test.com", "password"));

    Feed feed = saveFeed(user);
    // 다른 유저만 좋아요 생성
    em.persistAndFlush(new FeedLike(feed, otherUser));
    em.clear();

    // 작성자가 좋아요 누른 피드 id 목록
    Set<UUID> result = feedLikeRepository.findFeedIdsByUserId(userId, List.of(feed.getId()));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("좋아요를 눌렀어도 feedIds 목록에 없으면 반환되지 않는다")
  void findFeedIdsByUserId_likedButNotInFeedIds_notIncluded() {
    User user = em.find(User.class, userId);
    Feed likedFeed = saveFeed(user);
    Feed otherFeed = saveFeed(user);

    // likedFeed에 좋아요 생성
    em.persistAndFlush(new FeedLike(likedFeed, user));
    em.clear();

    // otherFeed에 좋아요 눌렀는지 검증
    Set<UUID> result = feedLikeRepository.findFeedIdsByUserId(userId, List.of(otherFeed.getId()));

    assertThat(result).isEmpty();
  }
}
