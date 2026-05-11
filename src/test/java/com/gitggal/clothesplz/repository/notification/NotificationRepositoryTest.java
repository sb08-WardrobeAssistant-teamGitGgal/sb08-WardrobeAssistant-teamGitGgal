package com.gitggal.clothesplz.repository.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitggal.clothesplz.config.QuerydslConfig;
import com.gitggal.clothesplz.entity.notification.Notification;
import com.gitggal.clothesplz.entity.notification.NotificationLevel;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.repository.RepositoryTestSupport;
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

@Import({QuerydslConfig.class, NotificationRepositoryTest.JpaAuditingConfig.class})
@DisplayName("알림 레포지토리 테스트")
class NotificationRepositoryTest extends RepositoryTestSupport {

  @TestConfiguration
  @EnableJpaAuditing
  static class JpaAuditingConfig {}

  @Autowired
  private TestEntityManager em;

  @Autowired
  private NotificationRepository notificationRepository;

  private User savedUser;

  @BeforeEach
  void setUp() {
    savedUser = em.persistAndFlush(new User("테스트유저", "test@test.com", "password"));
  }

  private Notification notification(User user) {
    return Notification.builder()
        .receiver(user)
        .title("알림 제목")
        .content("알림 내용")
        .level(NotificationLevel.INFO)
        .build();
  }

  // ─── findPage() ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("cursor가 없으면 최신 알림부터 limit개를 반환한다")
  void findPage_noCursor_returnsLatestByLimit() {
    // given
    em.persistAndFlush(notification(savedUser));
    em.persistAndFlush(notification(savedUser));
    em.persistAndFlush(notification(savedUser));

    // when
    List<Notification> result = notificationRepository.findPage(savedUser.getId(), null, null, 2);

    // then
    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("cursor가 있으면 이전 페이지 마지막 항목 이후의 알림만 반환한다")
  void findPage_withCursor_returnsNextPage() throws InterruptedException {
    // given - createdAt 차이를 보장하기 위해 1ms 간격으로 저장
    em.persistAndFlush(notification(savedUser));
    Thread.sleep(1);
    em.persistAndFlush(notification(savedUser));
    Thread.sleep(1);
    em.persistAndFlush(notification(savedUser));
    em.flush();
    em.clear();

    // 첫 번째 페이지 조회
    List<Notification> firstPage = notificationRepository.findPage(savedUser.getId(), null, null, 2);
    Notification last = firstPage.get(firstPage.size() - 1);

    // when - 두 번째 페이지 조회
    List<Notification> secondPage = notificationRepository.findPage(
        savedUser.getId(), last.getCreatedAt(), last.getId(), 2);

    // then - 첫 페이지와 겹치는 항목이 없어야 한다
    List<UUID> firstPageIds = firstPage.stream().map(Notification::getId).toList();
    assertThat(secondPage)
        .isNotEmpty()
        .noneMatch(n -> firstPageIds.contains(n.getId()));
  }

  @Test
  @DisplayName("다른 사용자의 알림은 조회되지 않는다")
  void findPage_doesNotReturnOtherUsersNotifications() {
    // given
    User otherUser = em.persistAndFlush(new User("다른유저", "other@test.com", "password"));
    em.persistAndFlush(notification(savedUser));
    em.persistAndFlush(notification(otherUser));

    // when
    List<Notification> result = notificationRepository.findPage(savedUser.getId(), null, null, 10);

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getReceiver().getId()).isEqualTo(savedUser.getId());
  }

  @Test
  @DisplayName("알림이 없으면 빈 리스트를 반환한다")
  void findPage_noNotifications_returnsEmptyList() {
    // when
    List<Notification> result = notificationRepository.findPage(savedUser.getId(), null, null, 10);

    // then
    assertThat(result).isEmpty();
  }

  // ─── countByReceiver_Id() ──────────────────────────────────────────────────

  @Test
  @DisplayName("해당 사용자의 전체 알림 수를 정확히 반환한다")
  void countByReceiver_Id_returnsCorrectCount() {
    // given
    em.persistAndFlush(notification(savedUser));
    em.persistAndFlush(notification(savedUser));
    em.persistAndFlush(notification(savedUser));

    // when
    long count = notificationRepository.countByReceiver_Id(savedUser.getId());

    // then
    assertThat(count).isEqualTo(3);
  }

  @Test
  @DisplayName("다른 사용자의 알림은 카운트에 포함되지 않는다")
  void countByReceiver_Id_excludesOtherUsersNotifications() {
    // given
    User otherUser = em.persistAndFlush(new User("다른유저", "other@test.com", "password"));
    em.persistAndFlush(notification(savedUser));
    em.persistAndFlush(notification(otherUser));
    em.persistAndFlush(notification(otherUser));

    // when
    long count = notificationRepository.countByReceiver_Id(savedUser.getId());

    // then
    assertThat(count).isEqualTo(1);
  }
}
