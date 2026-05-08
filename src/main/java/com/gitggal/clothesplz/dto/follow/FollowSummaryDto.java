package com.gitggal.clothesplz.dto.follow;

import java.util.UUID;

/**
 * 팔로우 요약 정보 DTO
 * 다른 사용자의 프로필 페이지에서 팔로워 수 등을 보여줄 때 사용
 *
 * 필드 설명
 * - followeeId     - 조회 대상 사용자 ID
 * - followerCount  - 해당 사용자를 팔로우하는 사람 수
 * - followingCount - 해당 사용자가 팔로우하고 있는 사람 수
 * - followedByMe   - 내가 해당 사용자를 팔로우하고 있는지 여부
 * - followedByMeId - 내가 팔로우 중이라면 그 팔로우 관계의 ID (언팔로우용)
 * - followingMe    - 해당 사용자가 나를 팔로우하고 있는지
 */
public record FollowSummaryDto(
    UUID followeeId,

    long followerCount,

    long followingCount,

    boolean followedByMe,

    UUID followedByMeId,

    boolean followingMe
) {
}
