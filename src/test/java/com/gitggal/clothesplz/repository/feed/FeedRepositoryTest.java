package com.gitggal.clothesplz.repository.feed;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitggal.clothesplz.config.QuerydslConfig;
import com.gitggal.clothesplz.dto.feed.FeedCursorCondition;
import com.gitggal.clothesplz.dto.feed.FeedDto;
import com.gitggal.clothesplz.dto.feed.FeedPageRequest;
import com.gitggal.clothesplz.entity.feed.Feed;
import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.weather.Location;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.SkyStatus;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.entity.weather.WindPhrase;
import com.gitggal.clothesplz.repository.RepositoryTestSupport;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
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
@DisplayName("피드 레포지토리 테스트")
public class FeedRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private FeedRepository feedRepository;

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

    User user = em.persistAndFlush(new User("작성자", "author@test.com", "password"));
    userId = user.getId();

    em.persistAndFlush(Profile.builder()
        .user(user)
        .imageUrl("http://profile.url")
        .build());

    em.clear();
  }

  private Feed saveFeed(String content) {
    Weather managedWeather = em.find(Weather.class, weatherId);
    User managedUser = em.find(User.class, userId);
    return em.persistAndFlush(new Feed(managedWeather, managedUser, List.of(), content));
  }

  private void setCreatedAt(UUID feedId, Instant createdAt) {
    em.getEntityManager()
        .createNativeQuery("UPDATE feeds SET created_at = ? WHERE id = ?")
        .setParameter(1, OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC))
        .setParameter(2, feedId)
        .executeUpdate();
  }

  private void setLikeCount(UUID feedId, long likeCount) {
    em.getEntityManager()
        .createNativeQuery("UPDATE feeds SET like_count = ? WHERE id = ?")
        .setParameter(1, likeCount)
        .setParameter(2, feedId)
        .executeUpdate();
  }

  private FeedPageRequest defaultRequest(int limit) {
    return new FeedPageRequest(null, null, limit, "createdAt", "DESCENDING",
        null, null, null, null);
  }

  // ===== findAllByCursor =====

  @Test
  @DisplayName("커서가 없으면 limit + 1개 반환한다")
  void findAllByCursor_noCursor_returnsLimitPlusOne() {
    saveFeed("피드1");
    saveFeed("피드2");
    saveFeed("피드3");
    em.clear();

    List<FeedDto> result = feedRepository.findAllByCursor(defaultRequest(2), new FeedCursorCondition(null, null, null), null);

    assertThat(result).hasSize(3);
  }

  @Test
  @DisplayName("피드 수가 limit 미만이면 있는 만큼만 반환한다")
  void findAllByCursor_lessThanLimit_returnsAll() {
    saveFeed("피드1");
    em.clear();

    List<FeedDto> result = feedRepository.findAllByCursor(defaultRequest(5), new FeedCursorCondition(null, null, null), null);

    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("createdAt 커서 이후 데이터만 반환한다 (내림차순)")
  void findAllByCursor_createdAtCursor_returnsOnlyAfterCursor() {
    Feed feed1 = saveFeed("피드1");
    Feed feed2 = saveFeed("피드2");
    Feed feed3 = saveFeed("피드3");

    Instant t1 = Instant.parse("2026-01-01T00:00:01Z");
    Instant t2 = Instant.parse("2026-01-01T00:00:02Z");
    Instant t3 = Instant.parse("2026-01-01T00:00:03Z");

    setCreatedAt(feed1.getId(), t1);
    setCreatedAt(feed2.getId(), t2);
    setCreatedAt(feed3.getId(), t3);
    em.clear();

    // t3 > t2 > t1 내림차순, 다음 페이지는 t1만 해당
    FeedPageRequest request = new FeedPageRequest(null, null, 10, "createdAt", "DESCENDING", null, null, null, null);
    FeedCursorCondition cursor = new FeedCursorCondition(t2, null, feed2.getId());
    List<FeedDto> result = feedRepository.findAllByCursor(request, cursor, null);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).content()).isEqualTo("피드1");
  }

  @Test
  @DisplayName("likeCount 커서 이후 데이터만 반환한다 (내림차순)")
  void findAllByCursor_likeCountCursor_returnsOnlyAfterCursor() {
    Feed feed1 = saveFeed("피드1");
    Feed feed2 = saveFeed("피드2");
    Feed feed3 = saveFeed("피드3");

    setLikeCount(feed1.getId(), 1L);
    setLikeCount(feed2.getId(), 2L);
    setLikeCount(feed3.getId(), 3L);
    em.clear();

    // 3 > 2 > 1 내림차순, likeCount=2인 feed2가 커서이므로 피드1만 해당
    FeedPageRequest request = new FeedPageRequest(null, null, 10, "likeCount", "DESCENDING", null, null, null, null);
    FeedCursorCondition cursor = new FeedCursorCondition(null, 2L, feed2.getId());
    List<FeedDto> result = feedRepository.findAllByCursor(request, cursor, null);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).content()).isEqualTo("피드1");
  }

  @Test
  @DisplayName("esMatchedIds로 해당 ID의 피드만 반환한다")
  void findAllByCursor_esMatchedIds_returnsOnlyMatchingFeeds() {
    Feed feed1 = saveFeed("오늘 날씨가 맑아서 좋다");
    saveFeed("비가 온다");
    em.clear();

    FeedPageRequest request = new FeedPageRequest(
        null, null, 10, "createdAt", "DESCENDING", null, null, null, null);
    List<FeedDto> result = feedRepository.findAllByCursor(request, new FeedCursorCondition(null, null, null), List.of(feed1.getId()));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).content()).isEqualTo("오늘 날씨가 맑아서 좋다");
  }

  @Test
  @DisplayName("authorId로 해당 작성자의 피드만 반환한다")
  void findAllByCursor_authorIdFilter_returnsOnlyAuthorFeeds() {
    saveFeed("작성자 피드");

    User otherUser = em.persistAndFlush(new User("다른유저", "other@test.com", "password"));
    em.persistAndFlush(Profile.builder().user(otherUser).imageUrl("http://other.url").build());
    Weather managedWeather = em.find(Weather.class, weatherId);
    em.persistAndFlush(new Feed(managedWeather, otherUser, List.of(), "다른유저 피드"));
    em.clear();

    FeedPageRequest request = new FeedPageRequest(
        null, null, 10, "createdAt", "DESCENDING", null, null, null, userId);
    List<FeedDto> result = feedRepository.findAllByCursor(request, new FeedCursorCondition(null, null, null), null);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).content()).isEqualTo("작성자 피드");
  }

  // ===== countByCondition =====

  @Test
  @DisplayName("조건 없이 전체 피드 수를 반환한다")
  void countByCondition_noFilter_returnsTotal() {
    saveFeed("피드1");
    saveFeed("피드2");
    saveFeed("피드3");
    em.clear();

    long count = feedRepository.countByCondition(defaultRequest(10), null);

    assertThat(count).isEqualTo(3);
  }

  @Test
  @DisplayName("esMatchedIds 조건 적용 시 해당 피드 수만 반환한다")
  void countByCondition_withEsMatchedIds_returnsMatchingCount() {
    Feed feed1 = saveFeed("봄 코디 공유");
    Feed feed2 = saveFeed("여름 코디 공유");
    saveFeed("운동 후기");
    em.clear();

    FeedPageRequest request = new FeedPageRequest(
        null, null, 10, "createdAt", "DESCENDING", null, null, null, null);
    long count = feedRepository.countByCondition(request, List.of(feed1.getId(), feed2.getId()));

    assertThat(count).isEqualTo(2);
  }
}
