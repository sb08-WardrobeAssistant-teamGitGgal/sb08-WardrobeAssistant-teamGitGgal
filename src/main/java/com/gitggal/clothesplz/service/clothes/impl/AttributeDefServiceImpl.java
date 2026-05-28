package com.gitggal.clothesplz.service.clothes.impl;

import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefCreateRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefDto;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefUpdateRequest;
import com.gitggal.clothesplz.entity.clothes.ClothesAttributeDef;
import com.gitggal.clothesplz.event.clothes.ClothingAttributeChangedEvent;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ClothesErrorCode;
import com.gitggal.clothesplz.mapper.clothes.AttributeDefMapper;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeDefRepository;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeRepository;
import com.gitggal.clothesplz.service.clothes.AttributeDefService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AttributeDefServiceImpl implements AttributeDefService {

  private final ClothesAttributeDefRepository clothesAttributeDefRepository;
  private final ClothesAttributeRepository clothesAttributeRepository;
  private final AttributeDefMapper attributeDefMapper;
  private final ApplicationEventPublisher eventPublisher;

  @Override
  @Transactional
  public ClothesAttributeDefDto createAttributeDef(
      ClothesAttributeDefCreateRequest request,
      UUID actorUserId
  ) {
    log.info("[Service] 의상 속성 생성 요청");

    if (clothesAttributeDefRepository.existsByName(request.name())) {
      log.error("[Service] 의상 속성 생성 실패 - 중복된 이름");
      throw new BusinessException(ClothesErrorCode.DUPLICATE_ATTRIBUTE_NAME);
    }

    ClothesAttributeDef attributeDef = new ClothesAttributeDef(
        request.name(),
        request.selectableValues()
    );
    ClothesAttributeDef savedAttributeDef;
    try {
      savedAttributeDef = clothesAttributeDefRepository.save(attributeDef);
    } catch (DataIntegrityViolationException e) {
      log.error("[Service] 의상 속성 생성 실패 - 중복된 이름");
      throw new BusinessException(ClothesErrorCode.DUPLICATE_ATTRIBUTE_NAME);
    }
    eventPublisher.publishEvent(new ClothingAttributeChangedEvent(
        actorUserId,
        savedAttributeDef.getName(),
        ClothingAttributeChangedEvent.ChangeType.ADDED
    ));

    log.info("[Service] 의상 속성 생성 완료");
    return attributeDefMapper.toClothesAttributeDefDto(savedAttributeDef);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClothesAttributeDefDto> getAttributeDefs(
      String sortBy,
      String sortDirection,
      String keywordLike
  ) {
    log.info("[Service] 의상 속성 조회 요청");

    List<ClothesAttributeDefDto> response = clothesAttributeDefRepository
        .findAllByConditions(sortBy, sortDirection, keywordLike).stream()
        .map(attributeDefMapper::toClothesAttributeDefDtoForSearch)
        .toList();

    log.info("[Service] 의상 속성 조회 완료");
    return response;
  }

  @Override
  @Transactional
  public void deleteAttributeDefs(UUID definitionId, UUID actorUserId) {
    log.info("[Service] 의상 속성 삭제 요청: definitionId={}", definitionId);

    ClothesAttributeDef attributeDef = clothesAttributeDefRepository.findById(definitionId)
        .orElseThrow(() -> new BusinessException(ClothesErrorCode.CLOTHES_ATTRIBUTE_DEFINITION_NOT_FOUND));

    String deletedName = attributeDef.getName();
    clothesAttributeRepository.deleteAllByDefinitionId(definitionId);
    clothesAttributeDefRepository.delete(attributeDef);
    eventPublisher.publishEvent(new ClothingAttributeChangedEvent(
        actorUserId,
        deletedName,
        ClothingAttributeChangedEvent.ChangeType.DELETED
    ));

    log.info("[Service] 의상 속성 삭제 완료: definitionId={}", definitionId);
  }

  @Override
  @Transactional
  public ClothesAttributeDefDto updateAttributeDef(
      UUID definitionId,
      ClothesAttributeDefUpdateRequest request,
      UUID actorUserId
  ) {
    log.info("[Service] 의상 속성 수정 시작");

    ClothesAttributeDef attributeDef = clothesAttributeDefRepository.findById(definitionId)
        .orElseThrow(() -> new BusinessException(ClothesErrorCode.CLOTHES_ATTRIBUTE_DEFINITION_NOT_FOUND));

    if (
        !attributeDef.getName().equals(request.name()) &&
        clothesAttributeDefRepository.existsByName(request.name())
    ) {
      log.error("[Service] 중복된 의상 속성 이름");
      throw new BusinessException(ClothesErrorCode.DUPLICATE_ATTRIBUTE_NAME);
    }

    attributeDef.update(request.name(), request.selectableValues());
    eventPublisher.publishEvent(new ClothingAttributeChangedEvent(
        actorUserId,
        attributeDef.getName(),
        ClothingAttributeChangedEvent.ChangeType.UPDATED
    ));

    log.info("[Service] 의상 속성 수정 완료");
    return attributeDefMapper.toClothesAttributeDefDto(attributeDef);
  }
}
