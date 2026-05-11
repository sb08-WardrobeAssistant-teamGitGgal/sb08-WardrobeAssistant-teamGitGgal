package com.gitggal.clothesplz.controller.auth;

import com.gitggal.clothesplz.security.jwt.JwtDto;
import com.gitggal.clothesplz.security.jwt.JwtInformation;
import com.gitggal.clothesplz.security.jwt.JwtTokenProvider;
import com.gitggal.clothesplz.service.auth.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  private final JwtTokenProvider jwtTokenProvider;

  @GetMapping("/csrf-token")
  public ResponseEntity<Void> getCsrfToken(CsrfToken csrfToken) {
    log.info("[Controller] CSRF 조회 요청 : CSRF 토큰 = {}", csrfToken.getToken());
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @PostMapping("/refresh")
  public ResponseEntity<JwtDto> refresh(@CookieValue("REFRESH_TOKEN") String refreshToken,
      HttpServletResponse response) {
    log.info("[Controller] 토큰 재발급 요청");
    JwtInformation jwtInformation = authService.refresh(refreshToken);
    ResponseCookie refreshCookie = jwtTokenProvider.generateRefreshTokenCookie(
        jwtInformation.refreshToken());
    response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

    JwtDto jwtDto = new JwtDto(jwtInformation.userDto(), jwtInformation.accessToken());
    return ResponseEntity.status(HttpStatus.OK).body(jwtDto);
  }
}
