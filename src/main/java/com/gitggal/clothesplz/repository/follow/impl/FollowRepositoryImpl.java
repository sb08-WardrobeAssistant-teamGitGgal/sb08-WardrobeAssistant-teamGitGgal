package com.gitggal.clothesplz.repository.follow.impl;

import static com.gitggal.clothesplz.entity.follow.QFollow.follow;

import com.gitggal.clothesplz.entity.follow.Follow;
import com.gitggal.clothesplz.repository.follow.FollowRepositoryCustom;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FollowRepositoryImpl implements FollowRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  /**
   * 내가 팔로우하는 목록
   */
  @Override
  public List<Follow> findFollowings(
      UUID followerId,
      String nameLike,
      Instant cursor,
      UUID idAfter,
      int limit) {

    return queryFactory
        .selectFrom(follow)

        .join(follow.followee).fetchJoin()
        .join(follow.follower).fetchJoin()

        .where(
            follow.follower.id.eq(followerId),

            // 이름 검색 조건
            nameLikeCondition(nameLike, follow.followee.name),

            // 커서 조건
            cursorCondition(cursor, idAfter)
        )
        .orderBy(
            follow.createdAt.desc(),
            follow.id.desc()
        )
        .limit(limit)
        .fetch();
  }

  /**
   * 나를 팔로우하는 목록
   */
  @Override
  public List<Follow> findFollowers(
      UUID followeeId,
      String nameLike,
      Instant cursor,
      UUID idAfter,
      int limit) {

    return queryFactory
        .selectFrom(follow)

        .join(follow.follower).fetchJoin()
        .join(follow.followee).fetchJoin()

        .where(
            follow.followee.id.eq(followeeId),

            // 이름 검색 조건
            nameLikeCondition(nameLike, follow.follower.name),

            // 커서 조건
            cursorCondition(cursor, idAfter)
        )
        .orderBy(
            follow.createdAt.desc(),
            follow.id.desc()
        )
        .limit(limit)
        .fetch();
  }

  /**
   * 이름 검색 조건
   */
  private BooleanExpression nameLikeCondition(String nameLike, StringPath namePath) {

    if (nameLike == null || nameLike.isBlank()) {
      return null;
    }

    return namePath.contains(nameLike);
  }

  /**
   * 커서 조건
   */
  private BooleanExpression cursorCondition(Instant cursor, UUID idAfter) {

    if (cursor == null) {
      return null;
    }

    return follow.createdAt.lt(cursor)
        .or (
            follow.createdAt.eq(cursor)
                .and(follow.id.lt(idAfter))
        );
  }
}
