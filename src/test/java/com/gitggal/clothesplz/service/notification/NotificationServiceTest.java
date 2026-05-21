package com.gitggal.clothesplz.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.gitggal.clothesplz.component.publisher.notification.NotificationPublisher;
import com.gitggal.clothesplz.dto.notification.NotificationDto;
import com.gitggal.clothesplz.dto.notification.NotificationDtoCursorResponse;
import com.gitggal.clothesplz.dto.notification.NotificationRequest;
import com.gitggal.clothesplz.entity.notification.Notification;
import com.gitggal.clothesplz.entity.notification.NotificationLevel;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.NotificationErrorCode;
import com.gitggal.clothesplz.mapper.notification.NotificationMapper;
import com.gitggal.clothesplz.repository.notification.NotificationRepository;
import com.gitggal.clothesplz.repository.user.UserRepository;
import com.gitggal.clothesplz.service.notification.impl.NotificationServiceImpl;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 서비스 테스트")
class NotificationServiceTest {

  @Mock
  private NotificationPublisher notificationPublisher;
  @Mock
  private UserRepository userRepository;
  @Mock
  private NotificationRepository notificationRepository;
  @Mock
  private NotificationMapper notificationMapper;

  @InjectMocks
  private NotificationServiceImpl notificationService;

  @Test
  @DisplayName("알림을 DB에 저장하고 Redis 채널에 발행한다")
  void send_savesAndPublishesToRedis() {
    // given
    UUID receiverId = UUID.randomUUID();
    NotificationRequest request = new NotificationRequest(receiverId, "제목", "내용", NotificationLevel.INFO);
    NotificationDto responseDto = new NotificationDto(UUID.randomUUID(), Instant.now(), receiverId, "제목", "내용", NotificationLevel.INFO);

    Notification saved = mock(Notification.class);

    given(userRepository.getReferenceById(receiverId)).willReturn(mock(User.class));
    given(notificationRepository.save(any(Notification.class))).willReturn(saved);
    given(notificationMapper.toDto(saved)).willReturn(responseDto);

    // when
    notificationService.send(request);

    // then
    then(notificationRepository).should().save(any(Notification.class));
    then(notificationPublisher).should().publish(responseDto);
  }

  @Test
  @DisplayName("오프라인 사용자도 DB 저장 후 Redis 채널에 발행한다")
  void send_offlineUser_savesAndPublishesToRedis() {
    // given
    UUID receiverId = UUID.randomUUID();
    NotificationRequest request = new NotificationRequest(
        receiverId, "제목", "내용", NotificationLevel.INFO);
    NotificationDto responseDto = new NotificationDto(
        UUID.randomUUID(), Instant.now(), receiverId, "제목", "내용", NotificationLevel.INFO);

    Notification saved = mock(Notification.class);

    given(userRepository.getReferenceById(receiverId)).willReturn(mock(User.class));
    given(notificationRepository.save(any(Notification.class))).willReturn(saved);
    given(notificationMapper.toDto(saved)).willReturn(responseDto);

    // when
    notificationService.send(request);

    // then
    then(notificationRepository).should().save(any(Notification.class));
    then(notificationPublisher).should().publish(responseDto);
  }

  @Test
  @DisplayName("다음 페이지가 있을 때 hasNext=true와 다음 커서를 반환한다")
  void getNotifications_hasNextPage_returnsNextCursor() {
    // given
    UUID receiverId = UUID.randomUUID();
    int limit = 2;

    Notification n1 = mock(Notification.class);
    Notification n2 = mock(Notification.class);
    Notification n3 = mock(Notification.class); // limit + 1 개 조회 → hasNext = true

    UUID lastId = UUID.randomUUID();
    Instant lastCreatedAt = Instant.now();
    given(n2.getId()).willReturn(lastId);
    given(n2.getCreatedAt()).willReturn(lastCreatedAt);

    given(notificationRepository.findPage(receiverId, null, null, limit + 1))
        .willReturn(List.of(n1, n2, n3));
    given(notificationRepository.countByReceiver_Id(receiverId)).willReturn(10L);
    given(notificationMapper.toDto(any(Notification.class))).willReturn(mock(NotificationDto.class));

    // when
    NotificationDtoCursorResponse response =
        notificationService.getNotifications(receiverId, null, null, limit);

    // then
    assertThat(response.hasNext()).isTrue();
    assertThat(response.nextCursor()).isEqualTo(lastCreatedAt.toString());
    assertThat(response.nextIdAfter()).isEqualTo(lastId);
    assertThat(response.data()).hasSize(limit);
  }

  @Test
  @DisplayName("마지막 페이지일 때 hasNext=false이고 nextCursor는 null이다")
  void getNotifications_lastPage_hasNextFalseAndNullCursor() {
    // given
    UUID receiverId = UUID.randomUUID();
    int limit = 5;

    given(notificationRepository.findPage(receiverId, null, null, limit + 1))
        .willReturn(List.of(mock(Notification.class)));
    given(notificationRepository.countByReceiver_Id(receiverId)).willReturn(1L);
    given(notificationMapper.toDto(any(Notification.class))).willReturn(mock(NotificationDto.class));

    // when
    NotificationDtoCursorResponse response =
        notificationService.getNotifications(receiverId, null, null, limit);

    // then
    assertThat(response.hasNext()).isFalse();
    assertThat(response.nextCursor()).isNull();
    assertThat(response.totalCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("잘못된 cursor 형식으로 요청하면 INVALID_CURSOR_FORMAT 예외가 발생한다")
  void getNotifications_invalidCursor_throwsInvalidCursorFormat() {
    // given
    UUID receiverId = UUID.randomUUID();
    String invalidCursor = "not-a-valid-instant";

    // when & then
    assertThatThrownBy(() ->
        notificationService.getNotifications(receiverId, invalidCursor, null, 10))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex ->
            assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(NotificationErrorCode.INVALID_CURSOR_FORMAT));
  }

  @Test
  @DisplayName("본인 알림을 정상적으로 삭제한다")
  void deleteNotification_owner_deletesSuccessfully() {
    // given
    UUID notificationId = UUID.randomUUID();
    UUID requesterId = UUID.randomUUID();

    User receiver = mock(User.class);
    Notification notification = mock(Notification.class);
    given(receiver.getId()).willReturn(requesterId);
    given(notification.getReceiver()).willReturn(receiver);
    given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

    // when
    notificationService.deleteNotification(notificationId, requesterId);

    // then
    then(notificationRepository).should().delete(notification);
  }

  @Test
  @DisplayName("존재하지 않는 알림 삭제 시 NOTIFICATION_NOT_FOUND 예외가 발생한다")
  void deleteNotification_notFound_throwsNotFound() {
    // given
    UUID notificationId = UUID.randomUUID();
    given(notificationRepository.findById(notificationId)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() ->
        notificationService.deleteNotification(notificationId, UUID.randomUUID()))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex ->
            assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
  }

  @Test
  @DisplayName("다른 사람의 알림 삭제 시 UNAUTHORIZED_NOTIFICATION_ACCESS 예외가 발생한다")
  void deleteNotification_notOwner_throwsUnauthorized() {
    // given
    UUID notificationId = UUID.randomUUID();
    UUID requesterId = UUID.randomUUID();
    UUID actualOwnerId = UUID.randomUUID(); // 다른 사람

    User receiver = mock(User.class);
    Notification notification = mock(Notification.class);
    given(receiver.getId()).willReturn(actualOwnerId);
    given(notification.getReceiver()).willReturn(receiver);
    given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

    // when & then
    assertThatThrownBy(() ->
        notificationService.deleteNotification(notificationId, requesterId))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex ->
            assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(NotificationErrorCode.UNAUTHORIZED_NOTIFICATION_ACCESS));
  }
}
