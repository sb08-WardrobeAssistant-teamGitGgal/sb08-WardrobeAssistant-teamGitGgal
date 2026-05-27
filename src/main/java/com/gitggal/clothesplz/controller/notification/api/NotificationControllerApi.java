package com.gitggal.clothesplz.controller.notification.api;

import com.gitggal.clothesplz.dto.notification.NotificationDtoCursorResponse;
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
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "알림", description = "알림 API")
public interface NotificationControllerApi {

  @Operation(summary = "알림 목록 조회", description = "알림 목록 조회 API")
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "200", description = "알림 목록 조회 성공",
          content = @Content(schema = @Schema(implementation = NotificationDtoCursorResponse.class))
      ),
      @ApiResponse(
          responseCode = "400", description = "알림 목록 조회 실패",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<NotificationDtoCursorResponse> getNotifications(
      @Parameter(description = "커서") String cursor,
      @Parameter(description = "커서 보조 ID") UUID idAfter,
      @Parameter(description = "페이지 크기", required = true) int limit,
      @Parameter(hidden = true) @AuthenticationPrincipal ClothesUserDetails userDetails
  );

  @Operation(summary = "알림 읽음 처리", description = "알림 읽음 처리 API")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "알림 읽음 처리 성공"),
      @ApiResponse(
          responseCode = "400", description = "알림 읽음 처리 실패",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class))
      )
  })
  ResponseEntity<Void> deleteNotification(
      @Parameter(description = "알림 ID", required = true) @PathVariable UUID notificationId,
      @Parameter(hidden = true) @AuthenticationPrincipal ClothesUserDetails userDetails
  );
}
