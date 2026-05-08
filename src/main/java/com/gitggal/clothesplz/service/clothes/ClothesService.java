package com.gitggal.clothesplz.service.clothes;

import com.gitggal.clothesplz.dto.clothes.ClothesCreateRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesDto;
import com.gitggal.clothesplz.dto.clothes.ClothesDtoCursorResponse;
import com.gitggal.clothesplz.dto.clothes.ClothesUpdateRequest;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ClothesService {

  ClothesDtoCursorResponse getClothes(
      String cursor,
      UUID idAfter,
      Integer limit,
      ClothesType typeEqual,
      UUID ownerId
  );

  ClothesDto createClothes(
      ClothesCreateRequest request,
      MultipartFile image
  );

  void deleteClothes(UUID clothesId);

  ClothesDto updateClothes(
      UUID clothesId,
      ClothesUpdateRequest request,
      MultipartFile image
  );

  ClothesDto extractByUrl(String url);
}
