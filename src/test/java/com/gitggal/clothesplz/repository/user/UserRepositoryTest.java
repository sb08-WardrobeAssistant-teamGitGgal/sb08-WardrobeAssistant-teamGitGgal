package com.gitggal.clothesplz.repository.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitggal.clothesplz.config.QuerydslConfig;
import com.gitggal.clothesplz.dto.user.UserDtoCursorRequest;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.user.UserRole;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
@Import(QuerydslConfig.class)
class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private EntityManager em;

  private User user1;
  private User user2;
  private User user3;

  private Instant t1;
  private Instant t2;
  private Instant t3;

  @BeforeEach
  void setUp() {
    user1 = new User("a", "a@test.com", "pw");
    userRepository.save(user1);

    user2 = new User("b", "b@test.com", "pw");
    userRepository.save(user2);

    user3 = new User("c", "c@test.com", "pw");
    userRepository.save(user3);
    user3.updateRole(UserRole.ADMIN);

    t1 = Instant.parse("2026-01-01T00:00:01Z");
    t2 = Instant.parse("2026-01-01T00:00:02Z");
    t3 = Instant.parse("2026-01-01T00:00:03Z");

    setCreatedAt(user1.getId(), t1);
    setCreatedAt(user2.getId(), t2);
    setCreatedAt(user3.getId(), t3);

    em.flush();
    em.clear();
  }

  private void setCreatedAt(UUID userId, Instant createdAt) {
    em.createNativeQuery(
            "UPDATE users SET created_at = :createdAt WHERE id = :id")
        .setParameter("createdAt", createdAt)
        .setParameter("id", userId)
        .executeUpdate();
  }

  @Test
  @DisplayName("이메일 검색")
  void getAllUsers_emailLike_success() {
    // given
    UserDtoCursorRequest request = new UserDtoCursorRequest(
        null,
        null,
        10,
        "email",
        "ASCENDING",
        "a",
        null,
        null
    );

    // when
    List<User> result = userRepository.getAllUsers(request);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getEmail()).isEqualTo("a@test.com");
  }

  @Test
  @DisplayName("역할 정렬")
  void getAllUsers_roleEqual_success() {
    // given
    UserDtoCursorRequest request = new UserDtoCursorRequest(
        null,
        null,
        10,
        "email",
        "ASCENDING",
        null,
        UserRole.ADMIN,
        null
    );

    // when
    List<User> result = userRepository.getAllUsers(request);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getRole()).isEqualTo(UserRole.ADMIN);
  }

  @Test
  @DisplayName("커서 기반 - 이메일 기준으로 오름차순으로 정렬")
  void getAllUsers_cursor_email_asc_success() {
    // given
    UserDtoCursorRequest request = new UserDtoCursorRequest(
        user1.getEmail(),
        user1.getId(),
        10,
        "email",
        "ASCENDING",
        null,
        null,
        null
    );

    // when
    List<User> result = userRepository.getAllUsers(request);

    // then
    assertThat(result).extracting(User::getEmail)
        .containsExactly("b@test.com", "c@test.com");
  }

  @Test
  @DisplayName("커서 기반 - 이메일 기준으로 내림차순 정렬")
  void getAllUsers_cursor_email_desc_success() {
    // given

    UserDtoCursorRequest request = new UserDtoCursorRequest(
        user3.getEmail(),
        user3.getId(),
        10,
        "email",
        "DESCENDING",
        null,
        null,
        null
    );

    // when
    List<User> result = userRepository.getAllUsers(request);

    // then
    assertThat(result).extracting(User::getEmail)
        .containsExactly("b@test.com", "a@test.com");
  }

  @Test
  @DisplayName("커서 기반 - 생성일 기준으로 오름차순 정렬")
  void getAllUsers_cursor_createdAt_asc_success() {
    // given
    UserDtoCursorRequest request = new UserDtoCursorRequest(
        t1.toString(),
        user1.getId(),
        10,
        "createdAt",
        "ASCENDING",
        null,
        null,
        null
    );

    // when
    List<User> result = userRepository.getAllUsers(request);

    // then
    assertThat(result).extracting(User::getEmail)
        .containsExactly( "b@test.com", "c@test.com");
  }

  @Test
  @DisplayName("커서 기반 - 생성일 기준으로 내림차순 정렬")
  void getAllUsers_cursor_createdAt_desc_success() {
    // given
    UserDtoCursorRequest request = new UserDtoCursorRequest(
        t3.toString(),
        user3.getId(),
        10,
        "createdAt",
        "DESCENDING",
        null,
        null,
        null
    );

    // when
    List<User> result = userRepository.getAllUsers(request);

    // then
    assertThat(result).extracting(User::getEmail)
        .containsExactly( "b@test.com", "a@test.com");
  }

  @Test
  @DisplayName("전체 개수")
  void totalCount() {
    UserDtoCursorRequest request = new UserDtoCursorRequest(
        null,
        null,
        10,
        "createdAt",
        "DESCENDING",
        null,
        null,
        null
    );

    long count = userRepository.totalCount(request);

    assertThat(count).isEqualTo(3);
  }
}