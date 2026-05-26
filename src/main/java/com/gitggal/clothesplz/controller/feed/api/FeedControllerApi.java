package com.gitggal.clothesplz.controller.feed.api;

import com.gitggal.clothesplz.dto.feed.CommentCreateRequest;
import com.gitggal.clothesplz.dto.feed.CommentDto;
import com.gitggal.clothesplz.dto.feed.CommentDtoCursorResponse;
import com.gitggal.clothesplz.dto.feed.CommentPageRequest;
import com.gitggal.clothesplz.dto.feed.FeedCreateRequest;
import com.gitggal.clothesplz.dto.feed.FeedDto;
import com.gitggal.clothesplz.dto.feed.FeedDtoCursorResponse;
import com.gitggal.clothesplz.dto.feed.FeedPageRequest;
import com.gitggal.clothesplz.dto.feed.FeedUpdateRequest;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "피드 관리", description = "피드 관련 API")
public interface FeedControllerApi {

  @Operation(summary = "피드 목록 조회", description = "피드 목록 조회 API")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200", description = "피드 목록 조회 성공",
          content = @Content(schema = @Schema(implementation = FeedDtoCursorResponse.class))
      ),
      @ApiResponse(
          responseCode = "400", description = "피드 목록 조회 실패",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<FeedDtoCursorResponse> getFeeds(
      @Valid @ParameterObject FeedPageRequest feedPageRequest,
      @Parameter(hidden = true) @AuthenticationPrincipal ClothesUserDetails userDetails
  );

  @Operation(summary = "피드 등록", description = "피드 등록 API")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "201", description = "피드 등록 성공",
          content = @Content(schema = @Schema(implementation = FeedDto.class))
      ),
      @ApiResponse(
          responseCode = "400", description = "피드 목록 조회 실패",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<FeedDto> create(
      @Valid @RequestBody FeedCreateRequest feedCreateRequest
  );

  @Operation(summary = "피드 좋아요", description = "피드 좋아요 API")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "204", description = "피드 좋아요 성공"
      ),
      @ApiResponse(
          responseCode = "400", description = "피드 좋아요 실패",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<Void> like(
      @Parameter(description = "feedId", required = true) @PathVariable UUID feedId,
      @Parameter(hidden = true) @AuthenticationPrincipal ClothesUserDetails userDetails
  );

  @Operation(summary = "피드 좋아요 취소", description = "피드 좋아요 취소 API")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "204", description = "피드 좋아요 취소 성공"
      ),
      @ApiResponse(
          responseCode = "400", description = "피드 좋아요 취소 실패",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<Void> cancelLike(
      @Parameter(description = "feedId", required = true) @PathVariable UUID feedId,
      @Parameter(hidden = true) @AuthenticationPrincipal ClothesUserDetails userDetails
  );

  @Operation(summary = "피드 댓글 조회", description = "피드 댓글 조회 API")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200", description = "피드 댓글 조회 성공",
          content = @Content(schema = @Schema(implementation = CommentDtoCursorResponse.class))
      ),
      @ApiResponse(
          responseCode = "400", description = "피드 댓글 조회 실패",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<CommentDtoCursorResponse> getComments(
      @Parameter(description = "feedId", required = true) @PathVariable UUID feedId,
      @Valid @ParameterObject CommentPageRequest commentPageRequest
  );

  @Operation(summary = "피드 댓글 등록", description = "피드 댓글 등록 API")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200", description = "피드 댓글 등록 성공",
          content = @Content(schema = @Schema(implementation = CommentDto.class))
      ),
      @ApiResponse(
          responseCode = "400", description = "피드 댓글 등록 실패",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<CommentDto> comment(
      @Parameter(description = "feedId", required = true) @PathVariable UUID feedId,
      @Valid @RequestBody CommentCreateRequest commentCreateRequest
  );

  @Operation(summary = "피드 삭제", description = "피드 삭제 API")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "204", description = "피드 삭제 성공"
      ),
      @ApiResponse(
          responseCode = "400", description = "피드 삭제 실패",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<Void> delete(
      @Parameter(description = "feedId", required = true) @PathVariable UUID feedId
  );

  @Operation(summary = "피드 수정", description = "피드 수정 API")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200", description = "피드 수정 성공",
          content = @Content(schema = @Schema(implementation = FeedDto.class))
      ),
      @ApiResponse(
          responseCode = "400", description = "피드 수정 실패",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<FeedDto> update(
      @Parameter(description = "feedId", required = true) @PathVariable UUID feedId,
      @Valid @RequestBody FeedUpdateRequest feedUpdateRequest
  );
}
