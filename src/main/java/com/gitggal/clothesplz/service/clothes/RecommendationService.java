package com.gitggal.clothesplz.service.clothes;

import com.gitggal.clothesplz.dto.clothes.RecommendationDto;
import com.gitggal.clothesplz.dto.user.UserDto;

public interface RecommendationService {

  RecommendationDto getRecommendations(String weatherId, UserDto user);
}
