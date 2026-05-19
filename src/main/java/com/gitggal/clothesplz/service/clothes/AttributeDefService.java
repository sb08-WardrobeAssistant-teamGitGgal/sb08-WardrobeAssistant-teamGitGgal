package com.gitggal.clothesplz.service.clothes;

import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefCreateRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefDto;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefUpdateRequest;
import java.util.List;
import java.util.UUID;

public interface AttributeDefService {

  ClothesAttributeDefDto createAttributeDef(ClothesAttributeDefCreateRequest request);

  List<ClothesAttributeDefDto> getAttributeDefs(
      String sortBy,
      String sortDirection,
      String keywordLike
  );

  void deleteAttributeDefs(UUID definitionId);

  ClothesAttributeDefDto updateAttributeDef(UUID definitionId, ClothesAttributeDefUpdateRequest request);
}
