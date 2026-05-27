package com.gitggal.clothesplz.controller.follow.api;

import com.gitggal.clothesplz.dto.follow.FollowCreateRequest;
import com.gitggal.clothesplz.dto.follow.FollowDto;
import com.gitggal.clothesplz.dto.follow.FollowListResponse;
import com.gitggal.clothesplz.dto.follow.FollowSummaryDto;
import com.gitggal.clothesplz.exception.ErrorResponse;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "팔로우 관리", description = "팔로우 관련 API")
public interface FollowControllerApi {

  @Operation(summary = "팔로우 생성", description = "팔로우 생성 API")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201", description = "팔로우 생성 성공",
          content = @Content(schema = @Schema(implementation = FollowDto.class))
      ),
      @ApiResponse(
          responseCode = "400", description = "팔로우 생성 실패",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<FollowDto> createFollow(
      @Valid @RequestBody FollowCreateRequest request
  );

  @Operation(summary = "팔로우 취소", description = "팔로우 취소 API")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "팔로우 취소 성공"),
      @ApiResponse(
          responseCode = "400", description = "팔로우 취소 실패",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<Void> cancelFollow(
      @Parameter(description = "팔로우 ID", required = true) @PathVariable UUID followId
  );

  @Operation(summary = "팔로잉 목록 조회", description = "팔로잉 목록 조회 API")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200", description = "팔로잉 목록 조회 성공",
          content = @Content(schema = @Schema(implementation = FollowListResponse.class))
      ),
      @ApiResponse(
          responseCode = "400", description = "팔로잉 목록 조회 실패",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<FollowListResponse> getFollowings(
      @Parameter(description = "팔로워 ID", required = true) UUID followerId,
      @Parameter(description = "이름 검색어") String nameLike,
      @Parameter(description = "커서") String cursor,
      @Parameter(description = "커서 보조 ID") UUID idAfter,
      @Parameter(description = "페이지 크기", required = true) int limit
  );

  @Operation(summary = "팔로워 목록 조회", description = "팔로워 목록 조회 API")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200", description = "팔로워 목록 조회 성공",
          content = @Content(schema = @Schema(implementation = FollowListResponse.class))
      ),
      @ApiResponse(
          responseCode = "400", description = "팔로워 목록 조회 실패",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<FollowListResponse> getFollowers(
      @Parameter(description = "팔로우 대상 사용자 ID", required = true) UUID followeeId,
      @Parameter(description = "이름 검색어") String nameLike,
      @Parameter(description = "커서") String cursor,
      @Parameter(description = "커서 보조 ID") UUID idAfter,
      @Parameter(description = "페이지 크기", required = true) int limit
  );

  @Operation(summary = "팔로우 요약 정보 조회", description = "팔로우 요약 정보 조회 API")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200", description = "팔로우 요약 정보 조회 성공",
          content = @Content(schema = @Schema(implementation = FollowSummaryDto.class))
      ),
      @ApiResponse(
          responseCode = "400", description = "팔로우 조회 실패",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<FollowSummaryDto> getFollowSummary(
      @Parameter(description = "사용자 ID", required = true) UUID userId,
      @Parameter(hidden = true) @AuthenticationPrincipal ClothesUserDetails userDetails
  );
}
