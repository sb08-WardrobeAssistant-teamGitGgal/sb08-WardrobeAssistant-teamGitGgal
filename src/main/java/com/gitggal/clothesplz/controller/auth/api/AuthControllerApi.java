package com.gitggal.clothesplz.controller.auth.api;

import com.gitggal.clothesplz.dto.user.ResetPasswordRequest;
import com.gitggal.clothesplz.exception.ErrorResponse;
import com.gitggal.clothesplz.security.jwt.JwtDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "인증", description = "인증 관련 API")
public interface AuthControllerApi {

  @Operation(
      summary = "CSRF 토큰 조회",
      description = "클라이언트에서 사용할 CSRF 토큰을 발급합니다."
  )
  @ApiResponse(
      responseCode = "204",
      description = "CSRF 토큰 조회 성공"
  )
  ResponseEntity<Void> getCsrfToken(
      @Parameter(hidden = true)
      CsrfToken csrfToken
  );

  @Operation(
      summary = "토큰 재발급",
      description = "Refresh Token 쿠키를 이용하여 Access Token과 Refresh Token을 재발급합니다."
  )
  @ApiResponse(
      responseCode = "200",
      description = "토큰 재발급 성공",
      content = @Content(schema = @Schema(implementation = JwtDto.class))
  )
  @ApiResponse(
      responseCode = "401",
      description = "유효하지 않은 Refresh Token",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  ResponseEntity<JwtDto> refresh(
      @Parameter(
          description = "Refresh Token 쿠키",
          hidden = true
      )
      @CookieValue(value = "REFRESH_TOKEN", required = false)
      String refreshToken,

      @Parameter(hidden = true)
      HttpServletResponse response
  );

  @Operation(
      summary = "임시 비밀번호 발급",
      description = "가입된 이메일로 임시 비밀번호를 발급합니다."
  )
  @ApiResponse(
      responseCode = "204",
      description = "임시 비밀번호 발급 성공"
  )
  @ApiResponse(
      responseCode = "400",
      description = "잘못된 요청 값",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  @ApiResponse(
      responseCode = "404",
      description = "사용자를 찾을 수 없음",
      content = @Content(schema = @Schema(implementation = ErrorResponse.class))
  )
  ResponseEntity<Void> sendTempPassword(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          description = "임시 비밀번호 발급 요청 정보",
          content = @Content(
              schema = @Schema(implementation = ResetPasswordRequest.class)
          )
      )
      @Valid @RequestBody
      ResetPasswordRequest request
  );
}