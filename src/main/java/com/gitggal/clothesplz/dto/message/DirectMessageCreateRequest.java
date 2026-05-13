package com.gitggal.clothesplz.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * DM 송신 요청용 DTO
 */
public record DirectMessageCreateRequest(

    @NotNull(message = "senderId는 필수값입니다.")
    UUID senderId,

    @NotNull(message = "receiverId는 필수값입니다.")
    UUID receiverId,

    @NotBlank(message = "메시지 내용은 필수값입니다.")
    @Size(max = 100, message = "메시지 내용은 100자 이하여야 합니다.")
    String content
) {
}
