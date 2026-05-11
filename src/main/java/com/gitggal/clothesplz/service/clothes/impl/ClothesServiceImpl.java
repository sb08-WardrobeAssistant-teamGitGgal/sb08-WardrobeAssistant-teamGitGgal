package com.gitggal.clothesplz.service.clothes.impl;

import com.gitggal.clothesplz.dto.clothes.ClothesCreateRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesDto;
import com.gitggal.clothesplz.dto.clothes.ClothesDtoCursorResponse;
import com.gitggal.clothesplz.dto.clothes.ClothesGetRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesUpdateRequest;
import com.gitggal.clothesplz.mapper.clothes.ClothesMapper;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeDefRepository;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeRepository;
import com.gitggal.clothesplz.repository.clothes.ClothesRepository;
import com.gitggal.clothesplz.service.clothes.ClothesService;
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

  private final ClothesRepository clothesRepository;
  private final ClothesAttributeDefRepository clothesAttributeDefRepository;
  private final ClothesAttributeRepository clothesAttributeRepository;
  private final ClothesMapper clothesMapper;

  @Override
  @Transactional(readOnly = true)
  public ClothesDtoCursorResponse getClothes(ClothesGetRequest request) {

    return null;
  }

  @Override
  @Transactional
  public ClothesDto createClothes(ClothesCreateRequest request, MultipartFile image) {
    return null;
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
}
