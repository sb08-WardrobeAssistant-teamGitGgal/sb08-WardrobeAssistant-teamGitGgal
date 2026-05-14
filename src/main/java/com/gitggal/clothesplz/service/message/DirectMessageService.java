package com.gitggal.clothesplz.service.message;

import com.gitggal.clothesplz.dto.message.DirectMessageCreateRequest;
import com.gitggal.clothesplz.dto.message.DirectMessageDto;
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
}
