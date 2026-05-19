package com.gitggal.clothesplz.event.message;

import com.gitggal.clothesplz.dto.message.DirectMessageDto;
import java.util.UUID;

/**
 * 알림용 이벤트 클래스
 */
public record DirectMessageSentEvent(
    DirectMessageDto dto,
    UUID receiverId,
    String destination,
    String senderName
) {
}
