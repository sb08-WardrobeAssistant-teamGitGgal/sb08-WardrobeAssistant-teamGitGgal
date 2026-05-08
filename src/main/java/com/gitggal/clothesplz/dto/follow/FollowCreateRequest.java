package com.gitggal.clothesplz.dto.follow;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * 팔로우 생성 요청용 DTO
 */
public record FollowCreateRequest(

    @NotNull(message = "followerId는 필수값입니다.")
    UUID followerId,

    @NotNull(message = "followeeId는 필수값입니다.")
    UUID followeeId
) {

}
