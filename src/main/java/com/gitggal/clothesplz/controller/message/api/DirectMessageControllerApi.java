package com.gitggal.clothesplz.controller.message.api;

import com.gitggal.clothesplz.dto.message.DirectMessageDtoCursorResponse;
import com.gitggal.clothesplz.exception.ErrorResponse;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "DirectMessage", description = "DirectMessage API")
public interface DirectMessageControllerApi {

  @Operation(summary = "DM 목록 조회", description = "DM 목록 조회 API")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200", description = "DM 목록 조회 성공",
          content = @Content(schema = @Schema(implementation = DirectMessageDtoCursorResponse.class))
      ),
      @ApiResponse(
          responseCode = "400", description = "DM 목록 조회 실패",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<DirectMessageDtoCursorResponse> getMessages(
      @Parameter(description = "상대방 사용자 ID", required = true) UUID userId,
      @Parameter(description = "커서") String cursor,
      @Parameter(description = "커서 보조 ID") UUID idAfter,
      @Parameter(description = "페이지 크기", required = true) int limit,
      @Parameter(hidden = true) @AuthenticationPrincipal ClothesUserDetails userDetails
  );
}
