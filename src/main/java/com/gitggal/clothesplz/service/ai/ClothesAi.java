package com.gitggal.clothesplz.service.ai;

import com.gitggal.clothesplz.dto.clothes.ClothesDto;
import com.gitggal.clothesplz.entity.clothes.Clothes;
import com.gitggal.clothesplz.entity.weather.Weather;
import java.util.List;
import java.util.UUID;

public interface ClothesAi {

  /**
   * 현재 날씨와 의상 목록을 전달하면, 현재 날씨에 맞는 의상 ID를 반환한다
   *
   * @param weather 현재 날씨
   * @param allClothes 의상 목록
   * @return 현재 날씨에 맞는 의상 ID
   */
  List<UUID> recommendClothesIds(Weather weather, List<Clothes> allClothes, short tempSensitivity);

  /**
   * 구매 링크 URL을 전달하면 초기 의상 정보를 추출한다.
   *
   * @param url 구매 링크 URL
   * @return 추출된 의상 정보
   */
  ClothesDto extractClothesByUrl(String url);
}
