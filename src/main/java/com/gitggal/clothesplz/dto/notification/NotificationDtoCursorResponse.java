package com.gitggal.clothesplz.dto.notification;

import java.util.List;
import java.util.UUID;

/**
 * 알림 목록 조회 커서 페이지네이션 응답 DTO
 *
 * 필드 구성
 * - data               : 현재 알림 목록
 * - nextCursor         : 다음 커서, 현재 페이지 마지막 항목의 createdAt 값 (정렬 기준 필드의 값)
 * - nextIdAfter        : 다음 요청의 보조 커서 (동일 cursor 값 중복 처리용)
 * - hasNext            : 다음 페이지 존재 여부
 * - totalCount         : 전체 알림 수
 * - sortBy             : 정렬 기준 (기본값은 createdAt)
 * - sortDirection      : 정렬 방향 (기본값은 DESCENDING)
 */
public record NotificationDtoCursorResponse(
    List<NotificationDto> data,

    String nextCursor,

    UUID nextIdAfter,

    boolean hasNext,

    long totalCount,

    String sortBy,

    String sortDirection
) {

}
