package com.gitggal.clothesplz.mapper.clothes;

import com.gitggal.clothesplz.dto.clothes.ClothesAttributeWithDefDto;
import com.gitggal.clothesplz.dto.clothes.ClothesCreateRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesDto;
import com.gitggal.clothesplz.entity.clothes.Clothes;
import com.gitggal.clothesplz.entity.clothes.ClothesAttribute;
import com.gitggal.clothesplz.entity.clothes.ClothesAttributeDef;
import com.gitggal.clothesplz.entity.user.User;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ClothesMapper {

  @Mapping(target = "id", source = "clothes.id")
  @Mapping(target = "ownerId", source = "owner.id")
  @Mapping(target = "name", source = "clothes.name")
  @Mapping(target = "imageUrl", source = "clothes.imageUrl")
  @Mapping(target = "type", source = "clothes.type")
  ClothesDto toClothesDto(Clothes clothes, User owner, List<ClothesAttributeWithDefDto> attributes);

  @Mapping(target = "name", source = "request.name")
  @Mapping(target = "type", source = "request.type")
  Clothes toClothes(User owner, ClothesCreateRequest request, String imageUrl, String purchaseUrl);

  @Mapping(target = "definitionId", source = "attributeDef.id")
  @Mapping(target = "definitionName", source = "attributeDef.name")
  @Mapping(target = "selectableValues", source = "attributeDef.selectableValues")
  ClothesAttributeWithDefDto toClothesAttributeWithDefDto(ClothesAttributeDef attributeDef, String value);

  @Mapping(target = "definition", source = "attributeDef")
  ClothesAttribute toClothesAttribute(Clothes clothes, ClothesAttributeDef attributeDef, String value);
}
