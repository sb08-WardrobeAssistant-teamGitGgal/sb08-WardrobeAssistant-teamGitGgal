package com.gitggal.clothesplz.service.clothes.impl;

import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDto;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeWithDefDto;
import com.gitggal.clothesplz.dto.clothes.ClothesCreateRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesDto;
import com.gitggal.clothesplz.dto.clothes.ClothesDtoCursorResponse;
import com.gitggal.clothesplz.dto.clothes.ClothesGetRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesUpdateRequest;
import com.gitggal.clothesplz.entity.clothes.Clothes;
import com.gitggal.clothesplz.entity.clothes.ClothesAttribute;
import com.gitggal.clothesplz.entity.clothes.ClothesAttributeDef;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.CommonErrorCode;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.mapper.clothes.ClothesMapper;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeDefRepository;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeRepository;
import com.gitggal.clothesplz.repository.clothes.ClothesRepository;
import com.gitggal.clothesplz.repository.user.UserRepository;
import com.gitggal.clothesplz.service.clothes.ClothesService;
import com.gitggal.clothesplz.service.image.ImageUploader;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClothesServiceImpl implements ClothesService {

  private final UserRepository userRepository;
  private final ClothesRepository clothesRepository;
  private final ClothesAttributeDefRepository clothesAttributeDefRepository;
  private final ClothesAttributeRepository clothesAttributeRepository;
  private final ClothesMapper clothesMapper;
  private final ImageUploader imageUploader;

  @Override
  @Transactional(readOnly = true)
  public ClothesDtoCursorResponse getClothes(ClothesGetRequest request) {

    return null;
  }

  @Override
  @Transactional
  public ClothesDto createClothes(ClothesCreateRequest request, MultipartFile image) {

    User owner = findUserOrThrow(request.ownerId());
    String imageUrl = image != null ? imageUploader.upload(image) : null;
    List<ClothesAttributeDto> attributes = request.attributes();

    try {
      // 1. 옷 저장
      Clothes clothes = clothesRepository.save(
          clothesMapper.toClothes(owner, request, imageUrl, null));

      // 2. 요청의 속성 정의 ID 모음
      List<UUID> attributeIds = attributes.stream()
          .map(ClothesAttributeDto::definitionId)
          .toList();

      // 3. attributeIds로 ClothesAttributeDef 조회
      List<ClothesAttributeDef> attributeDefs = clothesAttributeDefRepository.findAllById(attributeIds);
      Map<UUID, ClothesAttributeDef> defById = attributeDefs.stream()
          .collect(Collectors.toMap(ClothesAttributeDef::getId, Function.identity()));

      if (defById.size() != attributeIds.size()) {
        throw new BusinessException(CommonErrorCode.INVALID_INPUT);
      }

      // 4. 조회된 ClothesAttributeDef, clothes, value로 ClothesAttribute 생성 및 저장
      List<ClothesAttribute> clothesAttributes = attributes.stream()
          .map(attribute -> {
            ClothesAttributeDef def = defById.get(attribute.definitionId());
            if (def == null) {
              throw new BusinessException(CommonErrorCode.INVALID_INPUT);
            }
            return clothesMapper.toClothesAttribute(clothes, def, attribute.value());
          })
          .toList();
      clothesAttributeRepository.saveAll(clothesAttributes);

      // 5. ClothesAttributeDef + value → ClothesAttributeWithDefDto 목록 변환
      List<ClothesAttributeWithDefDto> attributeWithDefDtos = clothesAttributes.stream()
          .map(attribute -> new ClothesAttributeWithDefDto(
              attribute.getDefinition().getId(),
              attribute.getDefinition().getName(),
              attribute.getDefinition().getSelectableValues(),
              attribute.getValue()))
          .toList();

      // 6. ClothesDto 변환 후 반환
      return clothesMapper.toClothesDto(clothes, owner, attributeWithDefDtos);
    } catch (RuntimeException e) {
      if (imageUrl != null) {
        imageUploader.delete(imageUrl);
      }
      throw e;
    }
  }


  @Override
  @Transactional
  public void deleteClothes(UUID clothesId) {

  }

  @Override
  @Transactional
  public ClothesDto updateClothes(
      UUID clothesId,
      ClothesUpdateRequest request,
      MultipartFile image
  ) {
    return null;
  }

  @Override
  @Transactional(readOnly = true)
  public ClothesDto extractByUrl(String url) {
    return null;
  }

  private User findUserOrThrow(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
  }
}
