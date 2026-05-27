package com.gitggal.clothesplz.service.clothes.impl;

import com.gitggal.clothesplz.dto.clothes.ClothesAttributeWithDefDto;
import com.gitggal.clothesplz.dto.clothes.OotdDto;
import com.gitggal.clothesplz.dto.clothes.RecommendationDto;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.clothes.Clothes;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.WeatherErrorCode;
import com.gitggal.clothesplz.mapper.clothes.ClothesMapper;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeRepository;
import com.gitggal.clothesplz.repository.clothes.ClothesRepository;
import com.gitggal.clothesplz.repository.profile.ProfileRepository;
import com.gitggal.clothesplz.repository.weather.WeatherRepository;
import com.gitggal.clothesplz.service.ai.ClothesAi;
import com.gitggal.clothesplz.service.clothes.RecommendationService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

  private final WeatherRepository weatherRepository;
  private final ClothesRepository clothesRepository;
  private final ClothesAttributeRepository clothesAttributeRepository;
  private final ProfileRepository profileRepository;
  private final ClothesMapper clothesMapper;
  private final ClothesAi clothesAi;

  @Override
  public RecommendationDto getRecommendations(UUID weatherId, UserDto user) {
    log.info("[Service] 의상 추천 조회 요청 시작");

    // 요청 weatherId의 날씨 조회
    Weather weather = weatherRepository.findById(weatherId)
        .orElseThrow(() -> new BusinessException(WeatherErrorCode.WEATHER_NOT_FOUND));

    // 소유하고 있는 옷
    List<Clothes> allClothes = clothesRepository.findByOwnerId(user.id());

    // 온도 민감도
    short tempSensitivity = profileRepository.findByUserId(user.id())
        .map(Profile::getTempSensitivity)
        .orElse((short) 3);

    // AI 추천 목록
    List<Clothes> recommended = recommendByLlm(weather, allClothes, tempSensitivity);

    // 추천 의상 목록으로 의상 속성 추출 -> 의상 ID : 속성 List 의 Map
    Map<UUID, List<ClothesAttributeWithDefDto>> attributesByClothesId =
        findAttributesByClothesId(recommended);

    // OOTD로 변환
    List<OotdDto> recommendedDtos = recommended.stream()
        .map(clothes -> clothesMapper.toOotdDto(
            clothes,
            attributesByClothesId.getOrDefault(clothes.getId(), List.of())
        ))
        .toList();

    log.info("[Service] 의상 추천 조회 요청 완료");

    return new RecommendationDto(weatherId.toString(), user.id().toString(), recommendedDtos);
  }

  private List<Clothes> recommendByLlm(Weather weather, List<Clothes> allClothes, short tempSensitivity) {
    if (allClothes.isEmpty()) {
      return List.of();
    }
    List<UUID> ids;
    try {
      // OpenAI를 통한 추천
      ids = clothesAi.recommendClothesIds(weather, allClothes, tempSensitivity);
    } catch (RuntimeException e) {
      log.error("[Service] LLM 추천 호출 실패: {}", e.getMessage(), e);
      return fallback(weather.getTemperatureCurrent(), allClothes);
    }

    List<Clothes> result = mapRecommended(ids, allClothes);
    if (!result.isEmpty()) {
      return result;
    }

    log.info("[Service] LLM 추천 없음, 규칙 기반 fallback 실행");
    return fallback(weather.getTemperatureCurrent(), allClothes);
  }

  private Map<UUID, List<ClothesAttributeWithDefDto>> findAttributesByClothesId(List<Clothes> clothesList) {
    List<UUID> clothesIds = clothesList.stream().map(Clothes::getId).toList();
    if (clothesIds.isEmpty()) {
      return Map.of();
    }

    return clothesAttributeRepository.findAllByClothesIdIn(clothesIds)
        .stream()
        .collect(Collectors.groupingBy(
            attr -> attr.getClothes().getId(),
            Collectors.mapping(
                attr -> clothesMapper.toClothesAttributeWithDefDto(
                    attr.getDefinition(), attr.getValue()
                ),
                Collectors.toList()
            )
        ));
  }

  private List<Clothes> mapRecommended(List<UUID> ids, List<Clothes> allClothes) {
    if (ids.isEmpty()) {
      return List.of();
    }

    Map<UUID, Clothes> clothesMap = allClothes.stream()
        .collect(Collectors.toMap(Clothes::getId, c -> c));

    return ids.stream()
        .distinct()
        .map(clothesMap::get)
        .filter(Objects::nonNull)
        .limit(5)
        .toList();
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
