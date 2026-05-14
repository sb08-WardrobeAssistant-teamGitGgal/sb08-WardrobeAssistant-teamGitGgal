package com.gitggal.clothesplz.repository.message.impl;

import static com.gitggal.clothesplz.entity.message.QDirectMessage.directMessage;

import com.gitggal.clothesplz.entity.message.DirectMessage;
import com.gitggal.clothesplz.repository.message.DirectMessageRepositoryCustom;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DirectMessageRepositoryImpl implements DirectMessageRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  /**
   * 두 사용자 간 DM 조회 (최신순)
   * @param userAId     - 사용자 A의 ID (요청자)
   * @param userBId     - 사용자 B의 ID (상대방)
   * @param cursor      - 이전 페이지 마지막 createdAt
   * @param idAfter     - 이전 페이지 마지막 id
   * @param limit       - 한 페이지 크기
   */
  @Override
  public List<DirectMessage> findPage(
      UUID userAId,
      UUID userBId,
      Instant cursor,
      UUID idAfter,
      int limit) {

    return queryFactory
        .selectFrom(directMessage)
        .where(
            betweenUsers(userAId, userBId),
            cursorCondition(cursor, idAfter)
        )
        .orderBy(
            directMessage.createdAt.desc(),
            directMessage.id.desc()
        )
        .limit(limit)
        .fetch();
  }

  /**
   * 두 사용자 간 전체 DM 개수
   */
  @Override
  public long countBetween(UUID userAId, UUID userBId) {

    Long count = queryFactory
        .select(directMessage.count())
        .from(directMessage)
        .where(betweenUsers(userAId, userBId))
        .fetchOne();

    return count == null ? 0L : count;
  }

  /**
   * 둘 사이의 메시지 조건
   * (sender=A AND receiver=B) OR (sender=B AND receiver=A)
   */
  private BooleanExpression betweenUsers(UUID a, UUID b) {
    return directMessage.sender.id.eq(a).and(directMessage.receiver.id.eq(b))
        .or(directMessage.sender.id.eq(b).and(directMessage.receiver.id.eq(a)));
  }

  /**
   * 커서 조건 조회
   */
  private BooleanExpression cursorCondition(Instant cursor, UUID idAfter) {

    if (cursor == null) {
      return null;
    }

    return directMessage.createdAt.lt(cursor)
        .or(directMessage.createdAt.eq(cursor)
            .and(directMessage.id.lt(idAfter)));
  }
}
