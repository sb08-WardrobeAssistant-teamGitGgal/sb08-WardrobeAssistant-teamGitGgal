package com.gitggal.clothesplz.service.clothes;

import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefCreateRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefDto;

public interface AttributeDefService {

  ClothesAttributeDefDto createAttributeDef(ClothesAttributeDefCreateRequest request);
}
