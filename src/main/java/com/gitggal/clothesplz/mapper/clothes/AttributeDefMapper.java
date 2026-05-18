package com.gitggal.clothesplz.mapper.clothes;

import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefDto;
import com.gitggal.clothesplz.entity.clothes.ClothesAttributeDef;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttributeDefMapper {

  ClothesAttributeDefDto toClothesAttributeDefDto(ClothesAttributeDef attributeDef);

  ClothesAttributeDefDto toClothesAttributeDefDtoForSearch(ClothesAttributeDef attributeDef);
}
