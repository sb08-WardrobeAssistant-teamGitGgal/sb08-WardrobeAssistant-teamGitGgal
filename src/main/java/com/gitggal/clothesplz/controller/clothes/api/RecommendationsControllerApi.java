package com.gitggal.clothesplz.controller.clothes.api;

import com.gitggal.clothesplz.dto.clothes.RecommendationDto;
import com.gitggal.clothesplz.exception.ErrorResponse;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "의상 추천", description = "의상 추천 관련 API")
public interface RecommendationsControllerApi {

  @Operation(summary = "의상 추천 조회", description = "날씨 정보를 기준으로 의상을 추천합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "의상 추천 조회 성공",
      content = @Content(schema = @Schema(implementation = RecommendationDto.class))
  )
  @ApiResponse(
      responseCode = "400",
      description = "요청 파라미터 오류",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  @ApiResponse(
      responseCode = "404",
      description = "날씨 정보 조회 실패",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  ResponseEntity<RecommendationDto> getRecommendations(
      @Parameter(description = "날씨 ID", required = true)
      UUID weatherId,
      @Parameter(hidden = true)
      @AuthenticationPrincipal ClothesUserDetails userDetails
  );
}
