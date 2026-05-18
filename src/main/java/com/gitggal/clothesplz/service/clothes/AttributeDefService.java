package com.gitggal.clothesplz.service.clothes;

import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefCreateRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefDto;
import java.util.List;

public interface AttributeDefService {

  ClothesAttributeDefDto createAttributeDef(ClothesAttributeDefCreateRequest request);

  List<ClothesAttributeDefDto> getAttributeDefs(
      String sortBy,
      String sortDirection,
      String keywordLike
  );
}
