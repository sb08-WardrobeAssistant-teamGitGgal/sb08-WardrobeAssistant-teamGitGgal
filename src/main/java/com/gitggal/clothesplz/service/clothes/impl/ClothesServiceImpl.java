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
import com.gitggal.clothesplz.exception.code.ClothesErrorCode;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.mapper.clothes.ClothesMapper;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeDefRepository;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeRepository;
import com.gitggal.clothesplz.repository.clothes.ClothesRepository;
import com.gitggal.clothesplz.repository.user.UserRepository;
import com.gitggal.clothesplz.service.clothes.ClothesService;
import com.gitggal.clothesplz.service.image.ImageUploader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    log.info("[Service] 의상 생성 요청 시작");

    UUID ownerId = request.ownerId();
    List<ClothesAttributeDto> attributes = request.attributes();

    User owner = findUserOrThrow(ownerId);
    String imageUrl = image != null ? imageUploader.upload(image) : null;

    try {
      Clothes clothes = clothesMapper.toClothes(owner, request, imageUrl, null);
      clothesRepository.save(clothes);

      List<UUID> attributeIds = attributes.stream().map(ClothesAttributeDto::definitionId).toList();
      if (attributeIds.size() != attributeIds.stream().distinct().count()) {
        log.error("[Service] 의상 생성 요청 실패 - 중복된 속성 정의 ID가 포함되었습니다.");
        throw new BusinessException(ClothesErrorCode.DUPLICATE_CLOTHES_ATTRIBUTE_DEFINITION_ID);
      }

      List<ClothesAttributeDef> clothesAttributes = clothesAttributeDefRepository.findAllById(attributeIds);
      if (clothesAttributes.size() != attributeIds.size()) {
        log.error("[Service] 의상 생성 요청 실패 - 존재하지 않는 속성 정의 ID가 포함되었습니다.");
        throw new BusinessException(ClothesErrorCode.CLOTHES_ATTRIBUTE_DEFINITION_NOT_FOUND);
      }

      // definitionId 기준으로 요청 속성을 빠르게 조회하기 위해 List를 Map으로 변환 -> O(1)
      Map<UUID, ClothesAttributeDto> requestAttributeByDefinitionId = new HashMap<>();
      for (ClothesAttributeDto attribute : attributes) {
        requestAttributeByDefinitionId.put(attribute.definitionId(), attribute);
      }

      List<ClothesAttribute> clothesAttributeList = new ArrayList<>();
      List<ClothesAttributeWithDefDto> attributeDef = new ArrayList<>();

      for (ClothesAttributeDef def : clothesAttributes) {
        String value = requestAttributeByDefinitionId.get(def.getId()).value();
        if (!def.getSelectableValues().contains(value)) {
          log.error("[Service] 의상 생성 요청 실패 - 허용되지 않는 속성 값입니다. definitionId={}, value={}", def.getId(), value);
          throw new BusinessException(ClothesErrorCode.INVALID_CLOTHES_ATTRIBUTE_VALUE);
        }

        clothesAttributeList.add(clothesMapper.toClothesAttribute(clothes, def, value));
        attributeDef.add(clothesMapper.toClothesAttributeWithDefDto(def, value));
      }
      clothesAttributeRepository.saveAll(clothesAttributeList);

      log.info("[Service] 의상 생성 요청 완료");
      return clothesMapper.toClothesDto(clothes, owner, attributeDef);
    } catch (RuntimeException e) {

      log.error("[Service] 의상 생성 요청 실패");
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
