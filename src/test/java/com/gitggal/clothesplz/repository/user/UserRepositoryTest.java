package com.gitggal.clothesplz.repository.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitggal.clothesplz.config.QuerydslConfig;
import com.gitggal.clothesplz.dto.user.UserDtoCursorRequest;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.user.UserRole;
import jakarta.persistence.EntityManager;
import java.util.List;
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

  @Test
  @DisplayName("이메일 검색")
  void getAllUsers_emailLike_success() {
    // given
    User user1 = new User("user1", "test@test.com", "pw");
    User user2 = new User("user2", "admin@test.com", "pw");

    userRepository.save(user1);
    userRepository.save(user2);

    em.flush();
    em.clear();

    UserDtoCursorRequest request = new UserDtoCursorRequest(
        null,
        null,
        10,
        "email",
        "ASCENDING",
        "ad",
        null,
        null
    );

    // when
    List<User> result = userRepository.getAllUsers(request);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getEmail()).isEqualTo("admin@test.com");
  }

  @Test
  @DisplayName("역할 정렬")
  void getAllUsers_roleEqual_success() {
    // given
    User admin = new User("admin", "admin@test.com", "pw");
    admin.updateRole(UserRole.ADMIN);

    User user = new User("user", "test@test.com", "pw");

    userRepository.save(admin);
    userRepository.save(user);

    em.flush();
    em.clear();

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
    User user1 = new User("a", "a@test.com", "pw");
    User user2 = new User("b", "b@test.com", "pw");
    User user3 = new User("c", "c@test.com", "pw");

    userRepository.save(user1);
    userRepository.save(user2);
    userRepository.save(user3);

    em.flush();
    em.clear();

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
    User user1 = new User("a", "a@test.com", "pw");
    User user2 = new User("b", "b@test.com", "pw");
    User user3 = new User("c", "c@test.com", "pw");

    userRepository.save(user1);
    userRepository.save(user2);
    userRepository.save(user3);

    em.flush();
    em.clear();

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
  void getAllUsers_cursor_createdAt_asc_success() throws InterruptedException {
    // given
    User user1 = new User("a", "a@test.com", "pw");
    userRepository.save(user1);

    Thread.sleep(2);
    User user2 = new User("b", "b@test.com", "pw");
    userRepository.save(user2);

    Thread.sleep(2);
    User user3 = new User("c", "c@test.com", "pw");
    userRepository.save(user3);

    em.flush();
    em.clear();

    UserDtoCursorRequest request = new UserDtoCursorRequest(
        user1.getCreatedAt().toString(),
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
        .containsExactly("b@test.com", "c@test.com");
  }

  @Test
  @DisplayName("커서 기반 - 생성일 기준으로 내림차순 정렬")
  void getAllUsers_cursor_createdAt_desc_success() throws InterruptedException {
    // given
    User user1 = new User("a", "a@test.com", "pw");
    userRepository.save(user1);

    Thread.sleep(2);
    User user2 = new User("b", "b@test.com", "pw");
    userRepository.save(user2);

    Thread.sleep(2);
    User user3 = new User("c", "c@test.com", "pw");
    userRepository.save(user3);

    em.flush();
    em.clear();

    UserDtoCursorRequest request = new UserDtoCursorRequest(
        user3.getCreatedAt().toString(),
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
        .containsExactly("b@test.com", "a@test.com");
  }
}