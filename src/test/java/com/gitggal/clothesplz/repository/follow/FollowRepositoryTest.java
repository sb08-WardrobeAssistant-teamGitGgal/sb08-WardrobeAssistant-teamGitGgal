package com.gitggal.clothesplz.repository.follow;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitggal.clothesplz.config.QuerydslConfig;
import com.gitggal.clothesplz.entity.follow.Follow;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.repository.RepositoryTestSupport;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Import(QuerydslConfig.class)
@EnableJpaAuditing
@DisplayName("Follow Repository 테스트")
public class FollowRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private FollowRepository followRepository;

  private User userA;
  private User userB;
  private User userC;

  @BeforeEach
  void setUp() {
    userA = em.persistAndFlush(new User("사용자A", "userA@test.com", "pass1234!"));
    userB = em.persistAndFlush(new User("사용자B", "userB@test.com", "pass1234!"));
    userC = em.persistAndFlush(new User("사용자C", "userC@test.com", "pass1234!"));
  }

  // 헬퍼 메서드 - 팔로우 생성 (반복 제거)
  private Follow follow(User follower, User followee) {
    return Follow.builder()
        .follower(follower)
        .followee(followee)
        .build();
  }

  // findByFollower_IdAndFollowee_Id 테스트
  // 두 사용자 간 팔로우 관계가 존재하는지 조회
  @Test
  @DisplayName("A가 B를 팔로우하면 findByFollower_IdAndFollowee_Id로 조회된다.")
  void findByFollowerAndFollowee() {

    // given: A -> B 팔로우 관계 저장
    em.persistAndFlush(follow(userA, userB));

    // when
    Optional<Follow> result = followRepository
        .findByFollower_IdAndFollowee_Id(userA.getId(), userB.getId());

    // then
    assertThat(result).isPresent();
    // follower가 A인지 검증
    assertThat(result.get().getFollower().getId()).isEqualTo(userA.getId());
  }

  @Test
  @DisplayName("팔로우 관계가 없으면 findByFollower_IdAndFollowee_Id는 빈 값을 반환한다.")
  void findByFollowerAndFollowee_Empty() {

    // when
    Optional<Follow> result = followRepository
        .findByFollower_IdAndFollowee_Id(userA.getId(), userB.getId());

    // then: Optional이 비어있어야 한다
    assertThat(result).isEmpty();
  }

  // existsByFollower_IdAndFollowee_Id 테스트
  // 팔로우 중복 여부 확인 (boolean)
  @Test
  @DisplayName("팔로우가 존재하면 existsByFollower_IdAndFollowee_Id는 true를 반환한다.")
  void exists_followExists_returnsTrue() {

    // given
    em.persistAndFlush(follow(userA, userB));

    // when
    boolean result = followRepository
        .existsByFollower_IdAndFollowee_Id(userA.getId(), userB.getId());

    // then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("팔로우가 없으면 existsByFollower_IdAndFollowee_Id는 false를 반환한다.")
  void exists_followNotExists_returnsFalse() {

    // when
    boolean result = followRepository
        .existsByFollower_IdAndFollowee_Id(userA.getId(), userB.getId());

    // then
    assertThat(result).isFalse();
  }

  // countByFollower_Id / countByFollowee_Id 테스트
  // 팔로잉/팔로워 수 카운트
  @Test
  @DisplayName("A가 B와 C를 팔로우하면 countByFollower_Id는 2를 반환한다.")
  void countByFollowerId_test_returnsTwo() {

    // given
    em.persistAndFlush(follow(userA, userB));
    em.persistAndFlush(follow(userA, userC));

    // when
    long count = followRepository.countByFollower_Id(userA.getId());

    // then
    assertThat(count).isEqualTo(2L);
  }

  @Test
  @DisplayName("B를 팔로우하는 사람이 없으면 countByFollowee_Id는 0을 반환한다.")
  void countByFolloweeId_test_returnsZero() {

    // when
    long count = followRepository.countByFollowee_Id(userB.getId());

    // then
    assertThat(count).isEqualTo(0L);
  }

  @Test
  @DisplayName("B를 A와 C 둘다 팔로우하면 countByFollowee_Id는 2를 반환한다.")
  void countByFolloweeId_test_returnsTwo() {

    // given
    em.persistAndFlush(follow(userA, userB));
    em.persistAndFlush(follow(userC, userB));

    // when
    long count = followRepository.countByFollowee_Id(userB.getId());

    // then
    assertThat(count).isEqualTo(2L);
  }

  // findFollowerIdsByFolloweeId 테스트
  // 특정 사람의 팔로워 ID 목록 조회 (피드 알림용)
  @Test
  @DisplayName("B의 팔로워가 A와 C이면 findFollowerIdsByFolloweeId는 A와 C의 ID를 반환한다.")
  void findFollowerIds_test_returnsBothIds() {

    // given
    em.persistAndFlush(follow(userA, userB));
    em.persistAndFlush(follow(userC, userB));

    // when
    List<UUID> ids = followRepository
        .findFollowerIdsByFolloweeId(userB.getId());

    assertThat(ids)
        .hasSize(2)
        .containsExactlyInAnyOrder(userA.getId(), userC.getId());
  }

  @Test
  @DisplayName("팔로워가 없으면 findFollowerIdsByFolloweeId는 빈 리스트를 반환한다.")
  void findFollowerIds_test_returnsEmpty() {

    // when
    List<UUID> ids = followRepository
        .findFollowerIdsByFolloweeId(userB.getId());

    // then
    assertThat(ids).isEmpty();
  }

  // findFollowings (QueryDSL 커스텀 쿼리) 테스트
  // A가 팔로우하는 목록을 조회
  @Test
  @DisplayName("커서 없이 팔로잉 목록을 조회하면 전체 목록이 반환된다.")
  void findFollowings_noCursor_returnsAll() {

    // given: (A -> B), (A -> C)
    em.persistAndFlush(follow(userA, userB));
    em.persistAndFlush(follow(userA, userC));

    // when
    List<Follow> result = followRepository
        .findFollowings(userA.getId(), null, null, null, 10);

    // then
    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("이름 검색 필터를 적용하면 이름이 포함된 팔로잉만 반환된다.")
  void findFollowings_withNameLike_returnsFiltered() {

    // given: (A -> B), (A -> C)
    em.persistAndFlush(follow(userA, userB));
    em.persistAndFlush(follow(userA, userC));

    // when: "사용자B" 검색
    List<Follow> result = followRepository
        .findFollowings(userA.getId(), "사용자B", null, null, 10);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getFollowee().getName()).isEqualTo("사용자B");
  }

  @Test
  @DisplayName("limit보다 많은 팔로잉이 있을 때 limit만큼만 반환된다")
  void findFollowings_limit_test_returnsLimitedResults() {

    // given: (A -> B), (A -> C)
    em.persistAndFlush(follow(userA, userB));
    em.persistAndFlush(follow(userA, userC));

    // when
    List<Follow> result = followRepository
        .findFollowings(userA.getId(), null, null, null, 1);

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("커서 이후의 팔로잉 목록만 조회된다")
  void findFollowings_withCursor_returnsAfterCursor() throws InterruptedException {

    // given: (A -> B), (A -> C) (시간 때문에 순서대로 생성)
    em.persistAndFlush(follow(userA, userB));

    Thread.sleep(100);

    Follow followAtoC = em.persistAndFlush(follow(userA, userC));

    Instant cursor = followAtoC.getCreatedAt();
    UUID idAfter = followAtoC.getId();

    // when
    List<Follow> result = followRepository
        .findFollowings(userA.getId(), null, cursor, idAfter, 10);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getFollowee().getId()).isEqualTo(userB.getId());
  }

  // findFollowers 테스트
  // 나를 팔로우하는 목록을 조회
  @Test
  @DisplayName("커서 없이 팔로워 목록을 조회하면 전체 목록이 반환된다.")
  void findFollowers_noCursor_returnsAll() {

    // given: (A -> B), (C -> B) (B의 팔로워가 A, C)
    em.persistAndFlush(follow(userA, userB));
    em.persistAndFlush(follow(userC, userB));

    // when
    List<Follow> result = followRepository
        .findFollowers(userB.getId(), null, null, null, 10);

    // then
    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("이름 검색 필터를 적용하면 이름이 포함된 팔로워만 반환된다.")
  void findFollowers_withNameLike_returnsFiltered() {

    // given: (A -> B), (C -> B) (B의 팔로워가 A, C)
    em.persistAndFlush(follow(userA, userB));
    em.persistAndFlush(follow(userC, userB));

    // when
    List<Follow> result = followRepository
        .findFollowers(userB.getId(), "사용자A", null, null, 10);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getFollower().getName()).isEqualTo("사용자A");
  }

  // countFollowings / countFollowers 테스트
  // 이름 필터가 적용된 팔로잉/팔로워 수 카운트
  @Test
  @DisplayName("이름 검색 없이 countFollowings를 호출하면 전체 팔로잉 수를 반환한다.")
  void countFollowings_noNameLike_returnsTotal() {

    // given: (A -> B), (A -> C)
    em.persistAndFlush(follow(userA, userB));
    em.persistAndFlush(follow(userA, userC));

    // when
    long count = followRepository.countFollowings(userA.getId(), null);

    // then
    assertThat(count).isEqualTo(2L);
  }

  @Test
  @DisplayName("이름 검색 필터로 countFollowings를 호출하면 필터된 팔로잉 수를 반환한다.")
  void countFollowings_withNameLike_returnsFiltered() {

    // given: (A -> B), (A -> C)
    em.persistAndFlush(follow(userA, userB));
    em.persistAndFlush(follow(userA, userC));

    // when
    long count = followRepository.countFollowings(userA.getId(), "사용자B");

    // then
    assertThat(count).isEqualTo(1L);
  }

  @Test
  @DisplayName("이름 검색 없이 countFollowers를 호출하면 전체 팔로워 수를 반환한다.")
  void countFollowers_noNameLike_returnsTotal() {

    // given: (A -> B), (A -> C)
    em.persistAndFlush(follow(userA, userB));
    em.persistAndFlush(follow(userC, userB));

    // when
    long count = followRepository.countFollowers(userB.getId(), null);

    // then
    assertThat(count).isEqualTo(2L);
  }

  @Test
  @DisplayName("팔로워가 없을 때 countFollowers는 0을 반환한다.")
  void countFollowers_noFollowers_returnsZero() {

    // when
    long count = followRepository.countFollowers(userB.getId(), null);

    // then
    assertThat(count).isEqualTo(0L);
  }
}
