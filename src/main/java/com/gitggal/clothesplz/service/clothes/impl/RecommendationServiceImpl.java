package com.gitggal.clothesplz.service.clothes.impl;

import com.gitggal.clothesplz.dto.clothes.ClothesDto;
import com.gitggal.clothesplz.dto.clothes.RecommendationDto;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.clothes.Clothes;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.CommonErrorCode;
import com.gitggal.clothesplz.exception.code.WeatherErrorCode;
import com.gitggal.clothesplz.mapper.clothes.ClothesMapper;
import com.gitggal.clothesplz.repository.clothes.ClothesRepository;
import com.gitggal.clothesplz.repository.weather.WeatherRepository;
import com.gitggal.clothesplz.service.clothes.RecommendationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

  private final WeatherRepository weatherRepository;
  private final ClothesRepository clothesRepository;
  private final ClothesMapper clothesMapper;

  @Override
  @Transactional(readOnly = true)
  public RecommendationDto getRecommendations(String weatherId, UserDto user) {
    log.info("[Service] 의상 추천 조회 요청 시작");

    UUID weatherUuid;

    try {
      weatherUuid = UUID.fromString(weatherId);
    } catch (IllegalArgumentException e) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, e);
    }

    Weather weather = weatherRepository.findById(weatherUuid)
        .orElseThrow(() -> new BusinessException(WeatherErrorCode.WEATHER_NOT_FOUND));
    List<Clothes> allClothes = clothesRepository.findByOwnerId(user.id());

    // [기본]
    // - 추우면 OUTER 우선
    // - 더우면 TOP 우선
    // - 그 외는 본인 옷 상위 10개
    List<Clothes> recommended;
    double currentTemp = weather.getTemperatureCurrent();

    if (currentTemp <= 10.0) {
      recommended = allClothes.stream()
          .filter(c -> c.getType() == ClothesType.OUTER)
          .limit(10)
          .toList();
    } else if (currentTemp >= 25.0) {
      recommended = allClothes.stream()
          .filter(c -> c.getType() == ClothesType.TOP)
          .limit(10)
          .toList();
    } else {
      recommended = allClothes.stream()
          .limit(10)
          .toList();
    }

    // fallback: 조건으로 걸러진 게 없으면 전체에서 N개
    if (recommended.isEmpty()) {
      recommended = allClothes.stream()
          .limit(10)
          .toList();
    }

    List<ClothesDto> recommendedDtos = recommended.stream()
        .map(c -> clothesMapper.toClothesDto(c, c.getOwner(), List.of()))
        .toList();

    log.info("[Service] 의상 추천 조회 요청 완료");

    return new RecommendationDto(
        weatherUuid.toString(),
        user.id().toString(),
        recommendedDtos
    );
  }
}
