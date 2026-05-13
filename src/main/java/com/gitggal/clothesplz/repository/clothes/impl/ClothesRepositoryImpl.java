package com.gitggal.clothesplz.repository.clothes.impl;

import com.gitggal.clothesplz.dto.clothes.ClothesGetRequest;
import com.gitggal.clothesplz.entity.clothes.Clothes;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import com.gitggal.clothesplz.entity.clothes.QClothes;
import com.gitggal.clothesplz.entity.clothes.QClothesAttribute;
import com.gitggal.clothesplz.entity.clothes.QClothesAttributeDef;
import com.gitggal.clothesplz.repository.clothes.ClothesRepositoryCustom;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ClothesRepositoryImpl implements ClothesRepositoryCustom {

  private final JPAQueryFactory factory;
  private final QClothes clothes = QClothes.clothes;
  private final QClothesAttribute clothesAttr = QClothesAttribute.clothesAttribute;
  private final QClothesAttributeDef clothesAttrDef = QClothesAttributeDef.clothesAttributeDef;

  @Override
  public List<Clothes> findAllByCursor(ClothesGetRequest request, Instant instantCursor) {
    return factory
        .selectFrom(clothes)
        .where(
            clothes.owner.id.eq(request.ownerId()),
            typeEq(request.typeEqual()),
            cursorCondition(instantCursor, request.idAfter())
        )
        .orderBy(
            clothes.createdAt.desc(),
            clothes.id.desc()
        )
        .limit(request.limit() + 1)
        .fetch();
  }

  @Override
  public Long countByCursor(ClothesGetRequest request) {
    return factory
        .select(clothes.count())
        .from(clothes)
        .where(
            clothes.owner.id.eq(request.ownerId()),
            typeEq(request.typeEqual())
        )
        .fetchOne();
  }

  // 옷 타입 null 체크
  private BooleanExpression typeEq(ClothesType type) {
    return type != null ? clothes.type.eq(type) : null;
  }

  // cursor null 체크
  private BooleanExpression cursorCondition(Instant cursor, UUID idAfter) {
    if (cursor == null) {
      return null;
    }

    return clothes.createdAt.lt(cursor)
        .or(clothes.createdAt.eq(cursor)
            .and(clothes.id.lt(idAfter)));
  }
}
