package com.gitggal.clothesplz.repository.user;

import static com.gitggal.clothesplz.entity.user.QUser.user;

import com.gitggal.clothesplz.dto.user.UserDtoCursorRequest;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.user.UserRole;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

  private final JPAQueryFactory jpaQueryFactory;

  @Override
  public List<User> getAllUsers(UserDtoCursorRequest request) {

    return jpaQueryFactory
        .selectFrom(user)
        .where(
            emailLikeCondition(request.emailLike()),
            roleEqualCondition(request.roleEqual()),
            lockedCondition(request.locked()),
            cursorCondition(request.cursor(), request.idAfter(), request.sortBy(),
                request.sortDirection())
        )
        .orderBy(
            orderByCondition(request.sortBy(), request.sortDirection())
        )
        .limit(request.limit() + 1)
        .fetch();
  }

  // 커서 조건
  private BooleanExpression cursorCondition(String cursor, UUID idAfter, String sortBy,
      String sortDirection) {
    if (cursor == null) {
      return null;
    }

    boolean isAscending = "ASCENDING".equalsIgnoreCase(sortDirection);

    if ("email".equalsIgnoreCase(sortBy)) {
      if (isAscending) {
        return user.email.gt(cursor)
            .or(user.email.eq(cursor).and(user.id.gt(idAfter)));
      } else {
        return user.email.lt(cursor)
            .or(user.email.eq(cursor).and(user.id.lt(idAfter)));
      }
    } else if ("createdAt".equalsIgnoreCase(sortBy)) {
      try {
        Instant cursorInstant = Instant.parse(cursor);
        if (isAscending) {
          return user.createdAt.gt(cursorInstant)
              .or(user.createdAt.eq(cursorInstant).and(user.id.gt(idAfter)));
        } else {
          return user.createdAt.lt(cursorInstant)
              .or(user.createdAt.eq(cursorInstant).and(user.id.lt(idAfter)));
        }
      } catch (DateTimeParseException e) {
        throw new BusinessException(UserErrorCode.INVALID_CURSOR_FORMAT);
      }
    }
    return null;
  }

  // 이메일
  private BooleanExpression emailLikeCondition(String emailLike) {
    if (emailLike == null || emailLike.isBlank()) {
      return null;
    }
    return user.email.containsIgnoreCase(emailLike);
  }

  // 역할
  private BooleanExpression roleEqualCondition(UserRole roleEqual) {
    if (roleEqual == null) {
      return null;
    }
    return user.role.eq(roleEqual);
  }

  // 잠금
  private BooleanExpression lockedCondition(Boolean locked) {
    if (locked == null) {
      return null;
    }
    return user.locked.eq(locked);
  }

  // 정렬
  private OrderSpecifier<?>[] orderByCondition(String sortBy, String sortDirection) {
    Order order = "ASCENDING".equalsIgnoreCase(sortDirection) ? Order.ASC : Order.DESC;

    List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

    if ("email".equalsIgnoreCase(sortBy)) {
      orderSpecifiers.add(new OrderSpecifier<>(order, user.email));
    } else if ("createdAt".equalsIgnoreCase(sortBy)) {
      orderSpecifiers.add(new OrderSpecifier<>(order, user.createdAt));
    }

    orderSpecifiers.add(new OrderSpecifier<>(order, user.id));

    return orderSpecifiers.toArray(new OrderSpecifier[0]);
  }

  @Override
  public long totalCount(UserDtoCursorRequest request){
    Long count =  jpaQueryFactory
        .select(user.count())
        .from(user)
        .where(
            emailLikeCondition(request.emailLike()),
            roleEqualCondition(request.roleEqual()),
            lockedCondition(request.locked())
        )
        .fetchOne();

    return count != null ? count : 0L;
  }

}
