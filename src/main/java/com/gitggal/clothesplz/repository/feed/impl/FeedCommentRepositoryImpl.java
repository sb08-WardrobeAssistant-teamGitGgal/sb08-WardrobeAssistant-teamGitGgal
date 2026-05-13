package com.gitggal.clothesplz.repository.feed.impl;

import static com.gitggal.clothesplz.entity.feed.QFeedComment.feedComment;
import static com.gitggal.clothesplz.entity.profile.QProfile.profile;
import static com.gitggal.clothesplz.entity.user.QUser.user;

import com.gitggal.clothesplz.dto.feed.CommentDto;
import com.gitggal.clothesplz.dto.feed.CommentPageRequest;
import com.gitggal.clothesplz.dto.user.AuthorDto;
import com.gitggal.clothesplz.repository.feed.FeedCommentRepositoryCustom;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedCommentRepositoryImpl implements FeedCommentRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<CommentDto> findAllByCursor(UUID feedId, CommentPageRequest commentPageRequest) {

    // 필요한 컬럼만 조회하기 위해 Projection 사용
    // DB에서 가져온 컬럼들을 CommentDto 생성자에 매핑
    return queryFactory
        .select(Projections.constructor(CommentDto.class,
            feedComment.id,
            feedComment.createdAt,
            feedComment.feed.id,
            Projections.constructor(AuthorDto.class,
                user.id,
                user.name,
                profile.imageUrl
            ),
            feedComment.content
        ))
        .from(feedComment)
        // 댓글의 사용자까지 가져오기
        .join(feedComment.author, user)
        // profile은 FeedComment와 연관 관계가 없기 때문에 on 조건으로 연결
        .join(profile).on(profile.user.eq(user))
        // 조회하고자 하는 feed와 cursor 조건
        .where(
            feedComment.feed.id.eq(feedId),
            combineCursorCondition(commentPageRequest)
        )
        // 최신순 정렬 고정 및 생성 시간 같을 경우 id 내림차순으로 tie breaker 사용
        .orderBy(feedComment.createdAt.desc(), feedComment.id.desc())
        // 다음 페이지 존재 여부 조회를 위한 + 1
        .limit(commentPageRequest.limit() + 1)
        .fetch();
  }

  // 커서 조건
  private BooleanExpression combineCursorCondition(CommentPageRequest commentPageRequest) {
    // 첫 페이지 요청인 경우 커서 조건 없이 처음부터 조회
    if (commentPageRequest.cursor() == null || commentPageRequest.idAfter() == null) {
      return null;
    }

    // 책갈피보다 생성 시간이 느린 댓글들부터 반환
    // 만약, 댓글의 생성 시간이 같을 경우 책갈피의 id보다 id가 작은 댓글들 반환
    Instant cursor = commentPageRequest.cursor();
    return feedComment.createdAt.lt(cursor)
        .or(feedComment.createdAt.eq(cursor).and(feedComment.id.lt(commentPageRequest.idAfter())));
  }
}
