package com.gitggal.clothesplz.service.clothes.impl;

import com.gitggal.clothesplz.service.clothes.OpenAiClient;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
  private final OpenAiClient openAiClient;

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

    // 의상 추천
    List<Clothes> recommended = recommendByLlm(weather, allClothes);

    // 의상 DTO 변환
    List<ClothesDto> recommendedDtos = recommended.stream()
        .map(c -> clothesMapper.toClothesDto(c, c.getOwner(), List.of()))
        .toList();

    log.info("[Service] 의상 추천 조회 요청 완료");

    return new RecommendationDto(weatherUuid.toString(), user.id().toString(), recommendedDtos);
  }

  private List<Clothes> recommendByLlm(Weather weather, List<Clothes> allClothes) {
    // OpenAI를 통한 추천
    List<UUID> ids = openAiClient.recommendClothesIds(weather, allClothes);

    if (!ids.isEmpty()) {
      // OpenAI에서 추천된 ID를 필터링
      Set<UUID> idSet = new HashSet<>(ids);
      List<Clothes> result = allClothes.stream()
          .filter(c -> idSet.contains(c.getId()))
          .limit(10)
          .toList();

      if (!result.isEmpty()) {
        return result;
      }
    }

    log.info("[Service] LLM 추천 없음, 규칙 기반 fallback 실행");
    return fallback(weather.getTemperatureCurrent(), allClothes);
  }

  /**
   * <p>온도 기반 자체 알고리즘</p>
   *
   * 10도 이하면 OUTER
   * 25도 이상이면 TOP
   * 그외에는 조회던 것 중 TOP 10
   *
   * @param temp
   * @param allClothes
   * @return
   */
  private List<Clothes> fallback(double temp, List<Clothes> allClothes) {
    ClothesType preferred = temp <= 10.0 ? ClothesType.OUTER
        : temp >= 25.0 ? ClothesType.TOP
        : null;

    List<Clothes> result = preferred == null ? List.of()
        : allClothes.stream().filter(c -> c.getType() == preferred).limit(10).toList();

    return result.isEmpty()
        ? allClothes.stream().limit(10).toList()
        : result;
  }
}
