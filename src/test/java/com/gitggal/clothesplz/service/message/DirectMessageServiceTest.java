package com.gitggal.clothesplz.service.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.gitggal.clothesplz.dto.follow.UserSummary;
import com.gitggal.clothesplz.dto.message.DirectMessageCreateRequest;
import com.gitggal.clothesplz.dto.message.DirectMessageDto;
import com.gitggal.clothesplz.dto.message.DirectMessageDtoCursorResponse;
import com.gitggal.clothesplz.dto.notification.NotificationRequest;
import com.gitggal.clothesplz.entity.message.DirectMessage;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.MessageErrorCode;
import com.gitggal.clothesplz.mapper.message.DirectMessageMapper;
import com.gitggal.clothesplz.repository.message.DirectMessageRepository;
import com.gitggal.clothesplz.repository.user.UserRepository;
import com.gitggal.clothesplz.service.message.impl.DirectMessageServiceImpl;
import com.gitggal.clothesplz.service.notification.NotificationService;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("DM 서비스 테스트")
public class DirectMessageServiceTest {

  @Mock
  private DirectMessageRepository directMessageRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private DirectMessageMapper directMessageMapper;
  @Mock
  private SimpMessagingTemplate messagingTemplate;
  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private DirectMessageServiceImpl directMessageService;

  @Test
  @DisplayName("정상 송신: DM을 저장하고, 푸시 + 알림을 발송한다.")
  void send_success() {

    // given
    UUID senderId = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();
    DirectMessageCreateRequest request =
        new DirectMessageCreateRequest(senderId, receiverId, "[TEST] 테스트용 메시지 입니다.");

    User sender = mock(User.class);
    User receiver = mock(User.class);

    given(sender.getName()).willReturn("Jun");
    given(userRepository.findById(senderId)).willReturn(Optional.of(sender));
    given(userRepository.findById(receiverId)).willReturn(Optional.of(receiver));

    DirectMessage saved = mock(DirectMessage.class);

    given(saved.getId()).willReturn(UUID.randomUUID());
    given(directMessageRepository.save(any(DirectMessage.class))).willReturn(saved);

    DirectMessageDto dto = new DirectMessageDto(
        UUID.randomUUID(),
        Instant.now(),
        new UserSummary(senderId, "Jun", null),
        new UserSummary(receiverId, "James", null),
        "[TEST] 테스트용 메시지 입니다.");

    given(directMessageMapper.toDto(saved)).willReturn(dto);

    // when
    DirectMessageDto result = directMessageService.send(request, senderId);

    // then
    assertThat(result).isEqualTo(dto);
    then(messagingTemplate).should().convertAndSend(anyString(), any(DirectMessageDto.class));
    then(notificationService).should().send(any(NotificationRequest.class));
  }

  @Test
  @DisplayName("위조 시도: 인증 userId와 senderId가 다르면 예외")
  void send_throws_whenSenderIdMismatch() {

    // given
    UUID senderId = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();
    UUID attackerId = UUID.randomUUID();

    DirectMessageCreateRequest request =
        new DirectMessageCreateRequest(senderId, receiverId, "[TEST] 테스트용 메시지 입니다.");

    assertThatThrownBy(() -> directMessageService.send(request, attackerId))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex ->
            assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(MessageErrorCode.UNAUTHORIZED_MESSAGE_ACCESS));

    then(directMessageRepository).should(never()).save(any());
    then(messagingTemplate).should(never()).convertAndSend(anyString(), (Object) any());
    then(notificationService).should(never()).send(any());
  }

  @Test
  @DisplayName("자기 자신에게 보내려 하면 예외")
  void send_throws_whenSelf() {

    // given
    UUID userId = UUID.randomUUID();

    DirectMessageCreateRequest request =
        new DirectMessageCreateRequest(userId, userId, "[TEST] 테스트용 메시지 입니다.");

    assertThatThrownBy(() -> directMessageService.send(request, userId))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex ->
            assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(MessageErrorCode.SELF_MESSAGE_NOT_ALLOWED));

    then(directMessageRepository).should(never()).save(any());
    then(messagingTemplate).should(never()).convertAndSend(anyString(), (Object) any());
    then(notificationService).should(never()).send(any());
  }

  @Test
  @DisplayName("정상 조회: DM 목록을 반환한다. (hasNext=false)")
  void getMessages_success() {

    // given
    UUID userId = UUID.randomUUID();
    UUID partnerId = UUID.randomUUID();

    DirectMessage message = mock(DirectMessage.class);

    given(directMessageRepository.findPage(userId, partnerId, null, null, 11))
        .willReturn(List.of(message));

    given(directMessageRepository.countBetween(userId, partnerId))
        .willReturn(1L);

    DirectMessageDto dto = new DirectMessageDto(
        UUID.randomUUID(),
        Instant.now(),
        new UserSummary(userId, "Jun", null),
        new UserSummary(partnerId, "James", null),
        "메시지 내용"
    );

    given(directMessageMapper.toDto(message)).willReturn(dto);

    // when
    DirectMessageDtoCursorResponse result =
        directMessageService.getMessages(userId, partnerId, null, null, 10);

    // then
    assertThat(result.hasNext()).isFalse();
    assertThat(result.totalCount()).isEqualTo(1L);
    assertThat(result.data()).hasSize(1);
  }

  @Test
  @DisplayName("결과가 limit+1개면 hasNext=true이고 nextCursor / nextIdAfter가 응답 값으로 출력된다.")
  void getMessages_hasNext_true() {

    // given
    UUID userId = UUID.randomUUID();
    UUID partnerId = UUID.randomUUID();

    DirectMessage message1 = mock(DirectMessage.class);
    DirectMessage message2 = mock(DirectMessage.class);
    DirectMessage message3 = mock(DirectMessage.class);

    Instant lastCreatedAt = Instant.now();
    UUID lastId = UUID.randomUUID();

    given(message2.getCreatedAt()).willReturn(lastCreatedAt);
    given(message2.getId()).willReturn(lastId);

    given(directMessageRepository.findPage(userId, partnerId, null, null, 3))
        .willReturn(List.of(message1, message2, message3));
    given(directMessageRepository.countBetween(userId, partnerId))
        .willReturn(3L);

    given(directMessageMapper.toDto(message1)).willReturn(mock(DirectMessageDto.class));
    given(directMessageMapper.toDto(message2)).willReturn(mock(DirectMessageDto.class));

    // when
    DirectMessageDtoCursorResponse result =
        directMessageService.getMessages(userId, partnerId, null, null, 2);


    // then
    assertThat(result.hasNext()).isTrue();
    assertThat(result.data()).hasSize(2);
    assertThat(result.nextCursor()).isEqualTo(lastCreatedAt.toString());
    assertThat(result.nextIdAfter()).isEqualTo(lastId);
  }

  @Test
  @DisplayName("cursor만 있고 idAfter가 null이면 INVALID_CURSOR_FORMAT 예외가 발생한다.")
  void getMessages_throws_invalidCursor() {

    // given
    UUID userId = UUID.randomUUID();
    UUID partnerId = UUID.randomUUID();

    // when & then
    assertThatThrownBy(() ->
        directMessageService.getMessages(userId, partnerId, "2026-05-15T00:00:00Z", null, 10))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex ->
            assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(MessageErrorCode.INVALID_CURSOR_FORMAT));
  }

  @Test
  @DisplayName("cursor 형식이 잘못되면 INVALID_CURSOR_FORMAT 예외가 발생한다.")
  void getMessages_throws_invalidCursorFormat() {

    // given
    UUID userId = UUID.randomUUID();
    UUID partnerId = UUID.randomUUID();
    UUID idAfter = UUID.randomUUID();

    // when & then
    assertThatThrownBy(() ->
        directMessageService.getMessages(userId, partnerId, "none-date", idAfter, 10))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex ->
            assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(MessageErrorCode.INVALID_CURSOR_FORMAT));
  }
}
