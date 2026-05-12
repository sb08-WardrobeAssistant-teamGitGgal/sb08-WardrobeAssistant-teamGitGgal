package com.gitggal.clothesplz.repository.feed;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitggal.clothesplz.config.QuerydslConfig;
import com.gitggal.clothesplz.dto.feed.CommentDto;
import com.gitggal.clothesplz.dto.feed.CommentPageRequest;
import com.gitggal.clothesplz.entity.feed.Feed;
import com.gitggal.clothesplz.entity.feed.FeedComment;
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
@DisplayName("피드 댓글 레포지토리 테스트")
class FeedCommentRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private FeedCommentRepository feedCommentRepository;

  private UUID userId;
  private UUID feedId;

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

    User user = em.persistAndFlush(new User("작성자", "author@test.com", "password"));
    userId = user.getId();

    em.persistAndFlush(Profile.builder()
        .user(user)
        .imageUrl("http://profile.url")
        .build());

    Feed feed = em.persistAndFlush(new Feed(weather, user, List.of(), "피드 내용"));
    feedId = feed.getId();

    em.clear();
  }

  private FeedComment saveComment(String content) {
    Feed managedFeed = em.find(Feed.class, feedId);
    User managedUser = em.find(User.class, userId);
    return em.persistAndFlush(new FeedComment(managedFeed, managedUser, content));
  }

  // 현재 feed의 CreatedAt이 update가 막혀있기 때문에 JPA로 수정 불가능
  // 따라서, native Query로 직접 DB를 조작
  private void setCreatedAt(UUID commentId, Instant createdAt) {
    em.getEntityManager()
        .createNativeQuery("UPDATE feed_comments SET created_at = ? WHERE id = ?")
        .setParameter(1, OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC))
        .setParameter(2, commentId)
        .executeUpdate();
  }

  @Test
  @DisplayName("커서가 없으면 limit + 1개 반환한다")
  void findAllByCursor_noCursor_returnsLimitPlusOne() {
    saveComment("댓글1");
    saveComment("댓글2");
    saveComment("댓글3");
    em.clear();

    CommentPageRequest request = new CommentPageRequest(null, null, 2);

    List<CommentDto> result = feedCommentRepository.findAllByCursor(feedId, request);

    // 다음 페이지 존재 여부 검증을 위한 limit + 1 확인
    assertThat(result).hasSize(3);
  }

  @Test
  @DisplayName("댓글 수가 limit 미만이면 있는 만큼만 반환한다")
  void findAllByCursor_lessThanLimit_returnsAll() {
    saveComment("댓글1");
    em.clear();

    CommentPageRequest request = new CommentPageRequest(null, null, 5);

    List<CommentDto> result = feedCommentRepository.findAllByCursor(feedId, request);

    assertThat(result).hasSize(1);
  }


  @Test
  @DisplayName("커서 이후 데이터만 반환한다")
  void findAllByCursor_withCursor_returnsOnlyAfterCursor() {
    FeedComment comment1 = saveComment("댓글1");
    FeedComment comment2 = saveComment("댓글2");
    FeedComment comment3 = saveComment("댓글3");

    Instant t1 = Instant.parse("2026-01-01T00:00:01Z");
    Instant t2 = Instant.parse("2026-01-01T00:00:02Z");
    Instant t3 = Instant.parse("2026-01-01T00:00:03Z");

    setCreatedAt(comment1.getId(), t1);
    setCreatedAt(comment2.getId(), t2);
    setCreatedAt(comment3.getId(), t3);
    em.clear();

    // t3 > t2 > t1 최신순, limit이 2기 때문에 다음 페이지는 t1만 해당
    CommentPageRequest request = new CommentPageRequest(t2.toString(), comment2.getId(), 2);

    List<CommentDto> result = feedCommentRepository.findAllByCursor(feedId, request);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).content()).isEqualTo("댓글1");
  }

  @Test
  @DisplayName("createdAt이 같을 때 id 내림차순으로 정렬된다")
  void findAllByCursor_sameCreatedAt_orderedByIdDesc() {
    FeedComment comment1 = saveComment("댓글1");
    FeedComment comment2 = saveComment("댓글2");

    Instant sameTime = Instant.parse("2026-01-01T00:00:00Z");
    setCreatedAt(comment1.getId(), sameTime);
    setCreatedAt(comment2.getId(), sameTime);
    em.clear();

    CommentPageRequest request = new CommentPageRequest(null, null, 10);

    List<CommentDto> result = feedCommentRepository.findAllByCursor(feedId, request);

    assertThat(result).hasSize(2);
    UUID firstId = result.get(0).id();
    UUID secondId = result.get(1).id();
    assertThat(firstId.toString().compareTo(secondId.toString())).isGreaterThan(0);
  }
}
