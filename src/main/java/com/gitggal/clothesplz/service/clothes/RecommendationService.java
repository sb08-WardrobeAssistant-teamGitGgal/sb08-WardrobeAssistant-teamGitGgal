package com.gitggal.clothesplz.service.clothes;

import com.gitggal.clothesplz.dto.clothes.RecommendationDto;
import com.gitggal.clothesplz.dto.user.UserDto;
import java.util.UUID;

public interface RecommendationService {

  RecommendationDto getRecommendations(UUID weatherId, UserDto user);
}
