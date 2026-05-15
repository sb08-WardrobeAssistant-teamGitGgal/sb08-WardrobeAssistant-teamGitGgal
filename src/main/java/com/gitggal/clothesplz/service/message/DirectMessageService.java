package com.gitggal.clothesplz.service.message;

import com.gitggal.clothesplz.dto.message.DirectMessageCreateRequest;
import com.gitggal.clothesplz.dto.message.DirectMessageDto;
import com.gitggal.clothesplz.dto.message.DirectMessageDtoCursorResponse;
import java.util.UUID;

/**
 * DM 서비스 인터페이스
 *
 */
public interface DirectMessageService {

  /**
   * DM 송신: 저장 + 푸시 + 알림 발송
   */
  DirectMessageDto send(DirectMessageCreateRequest request, UUID authUserId);

  /**
   * 두 사용자 간 DM 목록 조회
   */
  DirectMessageDtoCursorResponse getMessages(
      UUID userId,
      UUID partnerId,
      String cursor,
      UUID idAfter,
      int limit
  );
}
