package com.gitggal.clothesplz.service.message.impl;

import com.gitggal.clothesplz.dto.message.DirectMessageCreateRequest;
import com.gitggal.clothesplz.dto.message.DirectMessageDto;
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
        "[DM]" + sender.getName(),
        dto.content(),
        NotificationLevel.INFO);

    notificationService.send(notificationRequest);

    log.info("[Service] DM 송신 요청 완료: messageId={}", savedMessage.getId());

    return dto;
  }
}
