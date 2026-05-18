package com.gitggal.clothesplz.repository.message;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitggal.clothesplz.config.QuerydslConfig;
import com.gitggal.clothesplz.entity.message.DirectMessage;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.repository.RepositoryTestSupport;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Import({QuerydslConfig.class, DirectMessageRepositoryTest.JpaAuditingConfig.class})
@DisplayName("DM Repository 테스트")
public class DirectMessageRepositoryTest extends RepositoryTestSupport {

  @TestConfiguration
  @EnableJpaAuditing
  static class JpaAuditingConfig {}

  @Autowired
  private TestEntityManager em;

  @Autowired
  private DirectMessageRepository directMessageRepository;

  private User userA;
  private User userB;

  @BeforeEach
  void setUp() {
    userA = em.persist(new User("UserA", "a@test.com", "password1234!"));
    userB = em.persist(new User("UserB", "b@test.com", "password1234!"));
    em.flush();
  }

  @Test
  @DisplayName("cursor 없이 두 사용자 간 DM 목록을 조회한다.")
  void findPage_noCursor_returnsAll() {

    // given
    directMessageRepository.save(DirectMessage.builder()
        .sender(userA).receiver(userB).content("message1").build());

    // when
    List<DirectMessage> result = directMessageRepository
        .findPage(userA.getId(), userB.getId(), null, null, 10);

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("cursor 이전 메시지를 조회한다.")
  void findPage_withCursor_returnsMessagesBeforeCursor() {

    // given
    DirectMessage msg = directMessageRepository.save(DirectMessage.builder()
        .sender(userA).receiver(userB).content("message1").build());
    em.flush();

    Instant futureCursor = msg.getCreatedAt().plusSeconds(60);
    UUID idAfter = UUID.randomUUID();

    // when
    List<DirectMessage> result = directMessageRepository
        .findPage(userA.getId(), userB.getId(), futureCursor, idAfter, 10);

    // then
    assertThat(result).hasSize(1);
  }

  @Test
  @DisplayName("두 사용자 간 전체 DM 개수를 반환한다.")
  void countBetween_returnsCorrectCount() {

    // given
    directMessageRepository.save(DirectMessage.builder()
        .sender(userA).receiver(userB).content("message1").build());

    directMessageRepository.save(DirectMessage.builder()
        .sender(userB).receiver(userA).content("message2").build());

    // when
    long count = directMessageRepository.countBetween(userA.getId(), userB.getId());

    // then
    assertThat(count).isEqualTo(2L);
  }
}
