package com.gitggal.clothesplz.repository.clothes.impl;

import com.gitggal.clothesplz.entity.clothes.ClothesAttributeDef;
import com.gitggal.clothesplz.entity.clothes.QClothesAttributeDef;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeDefRepositoryCustom;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ClothesAttributeDefRepositoryImpl implements ClothesAttributeDefRepositoryCustom {

  private final JPAQueryFactory factory;
  private final QClothesAttributeDef attributeDef = QClothesAttributeDef.clothesAttributeDef;

  @Override
  public List<ClothesAttributeDef> findAllByConditions(
      String sortBy,
      String sortDirection,
      String keywordLike
  ) {
    return factory
        .selectFrom(attributeDef)
        .where(keywordContains(keywordLike))
        .orderBy(orderBy(sortBy, sortDirection))
        .fetch();
  }

  private BooleanExpression keywordContains(String keywordLike) {
    if (keywordLike == null || keywordLike.isBlank()) {
      return null;
    }
    return attributeDef.name.containsIgnoreCase(keywordLike.trim());
  }

  private OrderSpecifier<?> orderBy(String sortBy, String sortDirection) {
    ComparableExpressionBase<?> sortField = "name".equalsIgnoreCase(sortBy)
        ? attributeDef.name
        : attributeDef.createdAt;

    Order order = "ASCENDING".equalsIgnoreCase(sortDirection)
        ? Order.ASC
        : Order.DESC;

    return new OrderSpecifier<>(order, sortField);
  }
}
