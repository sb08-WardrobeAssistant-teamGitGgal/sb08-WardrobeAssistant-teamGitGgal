package com.gitggal.clothesplz.service.notification.impl;

import com.gitggal.clothesplz.dto.notification.NotificationDto;
import com.gitggal.clothesplz.dto.notification.NotificationDtoCursorResponse;
import com.gitggal.clothesplz.entity.notification.Notification;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.NotificationErrorCode;
import com.gitggal.clothesplz.mapper.notification.NotificationMapper;
import com.gitggal.clothesplz.repository.notification.NotificationRepository;
import com.gitggal.clothesplz.repository.notification.SseEmitterRepository;
import com.gitggal.clothesplz.repository.user.UserRepository;
import com.gitggal.clothesplz.service.notification.NotificationService;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

  private static final String SORT_BY = "createdAt";
  private static final String SORT_DIRECTION = "DESCENDING";
  private final SseEmitterRepository emitterRepository;
  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final NotificationMapper notificationMapper;

  /**
   * 알림 전송
   * <p>
   * 동작
   * - receiverId로 저장소에서 SseEmitter 조회
   * - emitter가 있으면 (현재 SSE 연결 중) -> send
   * - emitter가 없으면 (오프라인)         -> 아무것도 안 함
   *
   * @param dto - 전송할 알림 데이터
   */
  @Override
  @Transactional
  public void send(NotificationDto dto) {

    // User 프록시 생성 (SELECT 없음 — ID만 가진 프록시)
    User receiver = userRepository.getReferenceById(dto.receiverId());

    Notification notification = Notification.builder()
        .receiver(receiver)
        .title(dto.title())
        .content(dto.content())
        .level(dto.level())
        .build();

    Notification savedNotification = notificationRepository.save(notification);

    // SSE 연결 상태면 알림 전송
    emitterRepository
        .findByUserId(dto.receiverId())
        .ifPresent(emitter -> sendToEmitter(emitter, notificationMapper.toDto(savedNotification)));
  }

  /**
   * 알림 목록 조회 (커서 기반 페이지네이션)
   *
   * @param receiverId - 로그인한 사용자 ID
   * @param cursor     - 이전 페이지 마지막 createdAt
   * @param idAfter    - 이전 페이지 마지막 id
   * @param limit      - 한 페이지 크기
   */
  @Override
  public NotificationDtoCursorResponse getNotifications(
      UUID receiverId,
      String cursor,
      UUID idAfter,
      int limit) {

    Instant cursorInstant = null;

    if (cursor != null) {
      try {
        cursorInstant = Instant.parse(cursor);
      } catch (DateTimeParseException e) {
        throw new BusinessException((NotificationErrorCode.INVALID_CURSOR_FORMAT));
      }
    }

    List<Notification> notificationList =
        notificationRepository.findPage(receiverId, cursorInstant, idAfter, limit + 1);

    boolean hasNext = notificationList.size() > limit;

    // 다음 페이지 여부 판단 및 실제 반환 데이터 분리
    List<Notification> pageData = hasNext
        ? notificationList.subList(0, limit)
        : notificationList;

    // 다음 커서 계산
    String nextCursor = null;

    UUID nextIdAfter = null;

    if (hasNext && !pageData.isEmpty()) {
      Notification last = pageData.get(pageData.size() - 1);

      nextCursor = last.getCreatedAt().toString();

      nextIdAfter = last.getId();
    }

    // 전체 알림 수 조회
    long totalCount = notificationRepository.countByReceiver_Id(receiverId);

    List<NotificationDto> dtoList = pageData.stream()
        .map(notificationMapper::toDto)
        .toList();

    return new NotificationDtoCursorResponse(
        dtoList,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        SORT_BY,
        SORT_DIRECTION
    );
  }

  /**
   * 알림 읽음 처리 (삭제)
   * @param notificationId - 알림 ID
   * @param requesterId    - 요청한 사용자 ID
   */
  @Override
  @Transactional
  public void deleteNotification(UUID notificationId, UUID requesterId) {

    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

    // 본인 알림인지 확인
    if (!notification.getReceiver().getId().equals(requesterId)) {
      log.debug("[알림] 본인 알림이 아닙니다.: requesterId={}", requesterId);
      throw new BusinessException(NotificationErrorCode.UNAUTHORIZED_NOTIFICATION_ACCESS);
    }

    notificationRepository.delete(notification);

    log.debug("[알림] 읽음 처리 삭제 완료: id={}, requesterId={}", notificationId, requesterId);
  }

  /**
   * 실제 SseEmitter에 이벤트 전송 - 전송 실패 (저장소에서 제거)
   */
  private void sendToEmitter(SseEmitter emitter, NotificationDto dto) {
    try {
      emitter.send(
          SseEmitter.event()
              .id(dto.id().toString())
              .name("notifications")
              .data(dto)
      );
      log.debug("[알림] 전송 성공: receiverId={}, title={}", dto.receiverId(), dto.title());
    } catch (IOException e) {
      log.warn("[알림] 전송 실패: (연결 끊김): receiverId={}", dto.receiverId());
      emitterRepository.deleteByUserId(dto.receiverId());
    }
  }
}