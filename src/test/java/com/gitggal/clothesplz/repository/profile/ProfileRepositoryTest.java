package com.gitggal.clothesplz.repository.profile;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitggal.clothesplz.config.QuerydslConfig;
import com.gitggal.clothesplz.entity.profile.Gender;
import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.repository.RepositoryTestSupport;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Import(QuerydslConfig.class)
@EnableJpaAuditing
@DisplayName("Profile Repository 테스트")
class ProfileRepositoryTest extends RepositoryTestSupport {
  @Autowired
  private TestEntityManager em;

  @Autowired
  private ProfileRepository profileRepository;

  private User savedUser;

  @BeforeEach
  void setUp() {
    savedUser = em.persistAndFlush(new User("홍길동", "hong@test.com", "hong_password"));
  }

  private Profile profile(User user) {
    return Profile.builder()
        .user(user)
        .gender(Gender.MALE)
        .birthDate(LocalDate.of(1995, 1, 1))
        .latitude(37.5665)
        .longitude(126.9780)
        .gridX(60)
        .gridY(127)
        .build();
  }

  @Test
  @DisplayName("사용자에 해당하는 프로필을 반환한다")
  void findByUser_returnsProfile() {
    // given
    em.persistAndFlush(profile(savedUser));

    // when
    Optional<Profile> result = profileRepository.findByUser(savedUser);

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getUser().getId()).isEqualTo(savedUser.getId());
  }

  @Test
  @DisplayName("프로필이 없는 사용자는 empty를 반환한다")
  void findByUser_noProfile_returnsEmpty() {
    // when
    Optional<Profile> result = profileRepository.findByUser(savedUser);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("다른 사용자의 프로필은 조회되지 않는다")
  void findByUser_otherUsersProfile_returnsEmpty() {
    // given
    User otherUser = em.persistAndFlush(new User("임꺽정", "im@test.com", "im_password"));
    em.persistAndFlush(profile(otherUser));

    // when
    Optional<Profile> result = profileRepository.findByUser(savedUser);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("조회된 프로필의 User가 fetch join으로 함께 로딩된다")
  void findByUser_userIsFetchJoined() {
    // given
    em.persistAndFlush(profile(savedUser));
    em.clear(); // 영속성 관리에서 제거

    // when
    Optional<Profile> result = profileRepository.findByUser(savedUser);

    // then - LazyInitializationException 없이 User 접근 가능
    assertThat(result).isPresent();
    assertThat(result.get().getUser().getEmail()).isEqualTo("hong@test.com");
  }
}

