package com.gitggal.clothesplz.mapper.clothes;

import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefDto;
import com.gitggal.clothesplz.entity.clothes.ClothesAttributeDef;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttributeDefMapper {

  @Mapping(target = "definitionId", source = "id")
  @Mapping(target = "definitionName", source = "name")
  @Mapping(target = "selectableValues", source = "selectableValues")
  @Mapping(target = "value", ignore = true)
  ClothesAttributeDefDto toClothesAttributeDefDto(ClothesAttributeDef attributeDef);
}
