package com.gitggal.clothesplz.controller.clothes.api;

import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefCreateRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefDto;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefUpdateRequest;
import com.gitggal.clothesplz.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
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

  @Operation(summary = "의상 속성 정의 목록 조회", description = "의상 속성 정의 목록을 조회합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "의상 속성 정의 목록 조회 성공",
      content = @Content(array = @ArraySchema(schema = @Schema(implementation = ClothesAttributeDefDto.class)))
  )
  @ApiResponse(
      responseCode = "400",
      description = "필수 쿼리 파라미터(sortBy, sortDirection) 누락",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  @ApiResponse(
      responseCode = "401",
      description = "인증되지 않은 사용자",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  ResponseEntity<List<ClothesAttributeDefDto>> getAttributeDefs(
      String sortBy,
      String sortDirection,
      String keywordLike
  );

  @Operation(summary = "의상 속성 정의 삭제", description = "의상 속성 정의를 삭제합니다.")
  @ApiResponse(responseCode = "204", description = "의상 속성 정의 삭제 성공")
  @ApiResponse(
      responseCode = "400",
      description = "의상 속성 정의 삭제 실패",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  @ApiResponse(
      responseCode = "401",
      description = "인증되지 않은 사용자",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  @ApiResponse(
      responseCode = "403",
      description = "권한이 없는 사용자",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  ResponseEntity<Void> deleteAttributeDefs(
      @Parameter(description = "속성 정의 ID", required = true)
      UUID definitionId
  );

  @Operation(summary = "의상 속성 정의 수정", description = "의상 속성 정의를 수정합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "의상 속성 정의 수정 성공",
      content = @Content(schema = @Schema(implementation = ClothesAttributeDefDto.class))
  )
  @ApiResponse(
      responseCode = "400",
      description = "의상 속성 정의 수정 실패",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  @ApiResponse(
      responseCode = "401",
      description = "미인증 요청",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  @ApiResponse(
      responseCode = "403",
      description = "권한이 없는 사용자",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  ResponseEntity<ClothesAttributeDefDto> updateAttributeDefs(
      @Parameter(description = "속성 정의 ID", required = true)
      UUID definitionId,
      ClothesAttributeDefUpdateRequest request
  );
}
