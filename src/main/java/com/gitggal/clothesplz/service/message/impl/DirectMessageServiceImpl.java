package com.gitggal.clothesplz.service.message.impl;

import com.gitggal.clothesplz.dto.message.DirectMessageCreateRequest;
import com.gitggal.clothesplz.dto.message.DirectMessageDto;
import com.gitggal.clothesplz.dto.message.DirectMessageDtoCursorResponse;
import com.gitggal.clothesplz.dto.notification.NotificationRequest;
import com.gitggal.clothesplz.entity.message.DirectMessage;
import com.gitggal.clothesplz.entity.notification.NotificationLevel;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.MessageErrorCode;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.mapper.message.DirectMessageMapper;
import com.gitggal.clothesplz.repository.message.DirectMessageRepository;
import com.gitggal.clothesplz.repository.user.UserRepository;
import com.gitggal.clothesplz.service.message.DirectMessageService;
import com.gitggal.clothesplz.service.notification.NotificationService;
import com.gitggal.clothesplz.util.message.DmKeyGenerator;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DirectMessageServiceImpl implements DirectMessageService {

  private static final String SUB_PREFIX = "/sub/direct-messages_";
  private static final String SORT_BY = "createdAt";
  private static final String SORT_DIRECTION = "DESCENDING";

  private final DirectMessageRepository directMessageRepository;
  private final UserRepository userRepository;
  private final DirectMessageMapper directMessageMapper;
  private final SimpMessagingTemplate messagingTemplate;
  private final NotificationService notificationService;

  /**
   * DM 송신: 저장 + 푸시 + 알림 발송
   * @param request    - 클라이언트에서 보낸 DM 요청 정보
   * @param authUserId - STOMP CONNECT 시 인증된 사용자 ID
   */
  @Override
  @Transactional
  public DirectMessageDto send(DirectMessageCreateRequest request, UUID authUserId) {

    log.info("[Service] DM 송신 요청 시작: senderId={}, receiverId={}", request.senderId(), request.receiverId());

    // 인증된 사용자와 senderId 일치 확인
    if (!authUserId.equals(request.senderId())) {
      log.warn("[Service] DM 송신 실패: senderId 위조 시도: authUserId={}, senderId={}", authUserId, request.senderId());

      throw new BusinessException(MessageErrorCode.UNAUTHORIZED_MESSAGE_ACCESS);
    }

    // 자기 자신에게 DM 차단
    if (request.senderId().equals(request.receiverId())) {
      throw new BusinessException(MessageErrorCode.SELF_MESSAGE_NOT_ALLOWED);
    }

    User sender = userRepository.findById(request.senderId())
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    User receiver = userRepository.findById(request.receiverId())
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    // DM 저장
    DirectMessage message = DirectMessage.builder()
        .sender(sender)
        .receiver(receiver)
        .content(request.content())
        .build();

    DirectMessage savedMessage = directMessageRepository.save(message);

    DirectMessageDto dto = directMessageMapper.toDto(savedMessage);

    String dmKey = DmKeyGenerator.generateKey(request.senderId(), request.receiverId());

    String destination = SUB_PREFIX + dmKey;

    messagingTemplate.convertAndSend(destination, dto);

    log.info("[Service] DM 푸시: destination={}", destination);

    // 받는 사람에게 알림 발송
    NotificationRequest notificationRequest = new NotificationRequest(request.receiverId(),
        "[DM]" + " " + sender.getName(),
        dto.content(),
        NotificationLevel.INFO);

    notificationService.send(notificationRequest);

    log.info("[Service] DM 송신 요청 완료: messageId={}", savedMessage.getId());

    return dto;
  }

  /**
   * 두 사용자 간 DM 목록 조회
   * @param userId      - 요청자 ID
   * @param partnerId   - 상대방 ID
   * @param cursor      - 이전 페이지 마지막 createdAt
   * @param idAfter     - 이전 페이지 마지막 id
   * @param limit       - 한 페이지 크기
   */
  @Override
  public DirectMessageDtoCursorResponse getMessages(

      UUID userId,
      UUID partnerId,
      String cursor,
      UUID idAfter,
      int limit) {

    log.info("[Service] DM 목록 조회 요청 시작: userId={}, partnerId={}", userId, partnerId);

    Instant cursorInstant = parseCursor(cursor, idAfter);

    List<DirectMessage> messages = directMessageRepository
        .findPage(userId, partnerId, cursorInstant, idAfter, limit + 1);

    boolean hasNext = messages.size() > limit;

    List<DirectMessage> pageData = hasNext ? messages.subList(0, limit) : messages;

    String nextCursor = null;

    UUID nextIdAfter = null;

    if (hasNext && !pageData.isEmpty()) {

      DirectMessage lastMessage = pageData.get(pageData.size() - 1);

      nextCursor = lastMessage.getCreatedAt().toString();

      nextIdAfter = lastMessage.getId();
    }

    long totalCount = directMessageRepository.countBetween(userId, partnerId);

    List<DirectMessageDto> dtoList = pageData.stream()
        .map(directMessageMapper::toDto)
        .toList();

    log.info("[Service] DM 목록 조회 완료: totalCount={}", totalCount);

    return new DirectMessageDtoCursorResponse(
        dtoList,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        SORT_BY,
        SORT_DIRECTION
    );
  }

  private Instant parseCursor(String cursor, UUID idAfter) {

    if ((cursor == null) != (idAfter == null)) {
      throw new BusinessException(MessageErrorCode.INVALID_CURSOR_FORMAT);
    }

    if (cursor == null) return null;

    try {
      return Instant.parse(cursor);
    } catch (Exception e) {
      throw new BusinessException(MessageErrorCode.INVALID_CURSOR_FORMAT);
    }
  }
}
