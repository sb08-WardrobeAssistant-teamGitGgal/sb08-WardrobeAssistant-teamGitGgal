package com.gitggal.clothesplz.controller.clothes.api;

import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefCreateRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefDto;
import com.gitggal.clothesplz.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;


@Tag(name = "의상 속성 관리", description = "의상 관련 API")
public interface AttributeDefControllerApi {

  @Operation(summary = "의상 속성 정의 등록", description = "의상 속성 정의를 등록합니다.")
  @ApiResponse(
      responseCode = "201",
      description = "의상 속성 정의 등록 성공",
      content = @Content(schema = @Schema(implementation = ClothesAttributeDefDto.class))
  )
  @ApiResponse(
      responseCode = "400",
      description = "의상 속성 정의 등록 실패",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  ResponseEntity<ClothesAttributeDefDto> createAttributeDef(
      ClothesAttributeDefCreateRequest request
  );
}
