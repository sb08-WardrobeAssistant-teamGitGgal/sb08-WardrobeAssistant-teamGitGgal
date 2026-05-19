package com.gitggal.clothesplz.repository.message;

import com.gitggal.clothesplz.entity.message.DirectMessage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DM 커스텀 Repository (QueryDSL)
 */
public interface DirectMessageRepositoryCustom {

  /**
   * 두 사용자 간 DM 조회 (최신순)
   * @param userAId     - 사용자 A의 ID (요청자)
   * @param userBId     - 사용자 B의 ID (상대방)
   * @param cursor      - 이전 페이지 마지막 createdAt
   * @param idAfter     - 이전 페이지 마지막 id
   * @param limit       - 한 페이지 크기
   */
  List<DirectMessage> findPage(
      UUID userAId,
      UUID userBId,
      Instant cursor,
      UUID idAfter,
      int limit
  );

  /**
   * 두 사용자 간 전체 DM 개수
   */
  long countBetween(UUID userAId, UUID userBId);

}
