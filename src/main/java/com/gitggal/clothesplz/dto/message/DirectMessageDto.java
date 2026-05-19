package com.gitggal.clothesplz.dto.message;

import com.gitggal.clothesplz.dto.follow.UserSummary;
import java.time.Instant;
import java.util.UUID;

/**
 * DM 응답/푸시용 DTO
 */
public record DirectMessageDto(

    UUID id,

    Instant createdAt,

    UserSummary sender,

    UserSummary receiver,

    String content
) {
}
