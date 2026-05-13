package com.gitggal.clothesplz.controller.clothes;

import com.gitggal.clothesplz.dto.clothes.RecommendationDto;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import com.gitggal.clothesplz.service.clothes.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationsController {

  private final RecommendationService recommendationService;

  @GetMapping
  @PreAuthorize("isAuthenticated()")// 인증된 사용자만 접근 가능
  public ResponseEntity<RecommendationDto> getRecommendations(
      @RequestParam(value = "weatherId") String weatherId,
      @AuthenticationPrincipal ClothesUserDetails userDetails
  ) {
    log.info("[Controller] 의상 추천 조회 요청 시작");

    UserDto user = userDetails.getUserDto();

    RecommendationDto response = recommendationService.getRecommendations(weatherId, user);

    log.info("[Controller] 의상 추천 조회 요청 완료");
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(response);
  }
}
