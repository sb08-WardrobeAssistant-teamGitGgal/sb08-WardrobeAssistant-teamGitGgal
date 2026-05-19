package com.gitggal.clothesplz.repository.clothes;

import com.gitggal.clothesplz.entity.clothes.ClothesAttributeDef;
import java.util.List;

public interface ClothesAttributeDefRepositoryCustom {

  List<ClothesAttributeDef> findAllByConditions(
      String sortBy,
      String sortDirection,
      String keywordLike
  );
}
