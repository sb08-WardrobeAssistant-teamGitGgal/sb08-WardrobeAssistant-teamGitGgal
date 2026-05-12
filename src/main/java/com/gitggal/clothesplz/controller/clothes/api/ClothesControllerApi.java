package com.gitggal.clothesplz.controller.clothes.api;

import com.gitggal.clothesplz.dto.clothes.ClothesCreateRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesDto;
import com.gitggal.clothesplz.dto.clothes.ClothesDtoCursorResponse;
import com.gitggal.clothesplz.dto.clothes.ClothesGetRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesUpdateRequest;
import com.gitggal.clothesplz.exception.ErrorResponse;
import org.springdoc.core.annotations.ParameterObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "의상 관리", description = "의상 관련 API")
public interface ClothesControllerApi {

  @Operation(summary = "옷 목록 조회", description = "옷 목록을 조회합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "옷 목록 조회 성공",
      content = @Content(schema = @Schema(implementation = ClothesDtoCursorResponse.class))
  )
  @ApiResponse(
      responseCode = "400",
      description = "옷 목록 조회 실패",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  ResponseEntity<ClothesDtoCursorResponse> getClothes(
      @ParameterObject ClothesGetRequest request
  );

  @Operation(summary = "옷 등록", description = "새로운 옷을 등록합니다.")
  @ApiResponse(
      responseCode = "201",
      description = "옷 등록 성공",
      content = @Content(schema = @Schema(implementation = ClothesDto.class))
  )
  @ApiResponse(
      responseCode = "400",
      description = "옷 등록 실패",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  @RequestBody(
      required = true,
      description = "multipart/form-data: request(JSON) + image(file)",
      content = @Content(mediaType = "multipart/form-data")
  )
  ResponseEntity<ClothesDto> createClothes(
      ClothesCreateRequest request,
      MultipartFile image
  );

  @Operation(summary = "옷 삭제", description = "옷 정보를 삭제합니다.")
  @ApiResponse(responseCode = "204", description = "옷 삭제 성공")
  @ApiResponse(
      responseCode = "400",
      description = "옷 삭제 실패",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  ResponseEntity<Void> deleteClothes(
      @Parameter(description = "의상 ID", required = true)
      UUID clothesId
  );

  @Operation(summary = "옷 수정", description = "옷 정보를 수정합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "옷 수정 성공",
      content = @Content(schema = @Schema(implementation = ClothesDto.class))
  )
  @ApiResponse(
      responseCode = "400",
      description = "옷 수정 실패",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  @RequestBody(
      required = true,
      description = "multipart/form-data: request(JSON) + image(file)",
      content = @Content(mediaType = "multipart/form-data")
  )
  ResponseEntity<ClothesDto> updateClothes(
      @Parameter(description = "의상 ID", required = true)
      UUID clothesId,
      ClothesUpdateRequest request,
      MultipartFile image
  );

  @Operation(summary = "구매 링크로 옷 정보 불러오기", description = "구매 링크 URL로 옷 정보를 추출합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "구매 링크로 옷 정보 불러오기 성공",
      content = @Content(schema = @Schema(implementation = ClothesDto.class))
  )
  @ApiResponse(
      responseCode = "400",
      description = "구매 링크로 옷 정보 불러오기 실패",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  ResponseEntity<ClothesDto> extractByUrl(
      @Parameter(description = "구매 링크 URL", required = true)
      String url
  );
}
