package com.gitggal.clothesplz.repository.notification.impl;

import static com.gitggal.clothesplz.entity.notification.QNotification.notification;

import com.gitggal.clothesplz.entity.notification.Notification;
import com.gitggal.clothesplz.repository.notification.NotificationRepositoryCustom;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

/**
 * NotificationRepositoryCustom 구현체
 */
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Notification> findPage(
      UUID receiverId,
      Instant cursor,
      UUID idAfter,
      int limit) {

    return queryFactory
        .selectFrom(notification)
        .where(
            notification.receiver.id.eq(receiverId),
            cursorCondition(cursor, idAfter)
        )
        .orderBy(
            notification.createdAt.desc(),
            notification.id.desc()
        )
        .limit(limit)
        .fetch();
  }

  /**
   * 커서 조건 생성
   *
   * cursor가 null이면 해당 조건 자동 무시 -> 첫 번째 페이지
   * 두 번째 페이지 부터는 아래의 return 문 내용대로 진행된다.
   */
  private BooleanExpression cursorCondition(Instant cursor, UUID idAfter) {

    if (cursor == null) {
      return null;
    }

    return notification.createdAt.lt(cursor)
        .or (
            notification.createdAt.eq(cursor)
                .and(notification.id.lt(idAfter))
        );
  }
}
