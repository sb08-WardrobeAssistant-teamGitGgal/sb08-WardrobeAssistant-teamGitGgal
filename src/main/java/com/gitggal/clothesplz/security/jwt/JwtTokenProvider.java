package com.gitggal.clothesplz.security.jwt;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {

  public static final String REFRESH_TOKEN_COOKIE_NAME = "REFRESH_TOKEN";

  private final JwtProperties jwtProperties;
  private final JWSSigner accessTokenSigner;
  private final JWSVerifier accessTokenVerifier;
  private final JWSSigner refreshTokenSigner;
  private final JWSVerifier refreshTokenVerifier;

  public JwtTokenProvider(JwtProperties jwtProperties) throws JOSEException {
    this.jwtProperties = jwtProperties;

    byte[] accessSecretBytes = jwtProperties.accessTokenSecret().getBytes(StandardCharsets.UTF_8);
    this.accessTokenSigner = new MACSigner(accessSecretBytes);
    this.accessTokenVerifier = new MACVerifier(accessSecretBytes);

    byte[] refreshSecretBytes = jwtProperties.refreshTokenSecret().getBytes(StandardCharsets.UTF_8);
    this.refreshTokenSigner = new MACSigner(refreshSecretBytes);
    this.refreshTokenVerifier = new MACVerifier(refreshSecretBytes);
  }

  public String generateAccessToken(ClothesUserDetails userDetails) throws JOSEException {
    return generateToken(userDetails, jwtProperties.accessTokenExp(), accessTokenSigner, "access");
  }

  public String generateRefreshToken(ClothesUserDetails userDetails) throws JOSEException {
    return generateToken(userDetails, jwtProperties.refreshTokenExp(), refreshTokenSigner,
        "refresh");
  }

  // 토큰 생성
  private String generateToken(ClothesUserDetails userDetails, int expirationMs, JWSSigner signer,
      String type) throws JOSEException {
    String tokenId = UUID.randomUUID().toString();
    UUID userId = userDetails.getUserDto().id();
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expirationMs);

    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
        .subject(userId.toString())
        .jwtID(tokenId)
        .claim("userId", userId.toString())
        .claim("type", type)
        .claim("roles", userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList())
        .issueTime(now)
        .expirationTime(expiryDate)
        .build();

    SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
    signedJWT.sign(signer);
    String token = signedJWT.serialize();

    return token;
  }

  public boolean validateAccessToken(String token) {
    return validateToken(token, accessTokenVerifier, "access");
  }

  public boolean validateRefreshToken(String token) {
    return validateToken(token, refreshTokenVerifier, "refresh");
  }

  // 토큰 검증
  private boolean validateToken(String token, JWSVerifier verifier, String type) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);

      // 서명 검증
      if (!signedJWT.verify(verifier)) {
        log.debug("[JwtTokenProvider] 토큰 검증 실패 : 서명 검증 실패");
        return false;
      }

      // 토큰 타입 검증
      String tokenType = (String) signedJWT.getJWTClaimsSet().getClaim("type");
      if (!type.equals(tokenType)) {
        log.debug("[JwtTokenProvider] 토큰 검증 실패 : 토큰 타입 검증 실패");
        return false;
      }

      // 만료시간 검증
      Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
      if (expirationTime == null || expirationTime.before(new Date())) {
        log.debug("[JwtTokenProvider] 토큰 검증 실패 : 만료시간 검증 실패");
        return false;
      }

      return true;
    } catch (Exception e) {
      log.debug("[JwtTokenProvider] 토큰 검증 실패");
      return false;
    }
  }

  public UUID getUserId(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      String userIdStr = signedJWT.getJWTClaimsSet().getSubject();
      if (userIdStr == null) {
        throw new BusinessException(UserErrorCode.JWT_TOKEN_PARSE_FAILED);
      }
      return UUID.fromString(userIdStr);
    } catch (ParseException e) {
      throw new BusinessException(UserErrorCode.JWT_TOKEN_PARSE_FAILED, e);
    }
  }

  public String getTokenId(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      return signedJWT.getJWTClaimsSet().getJWTID();
    } catch (ParseException e) {
      throw new BusinessException(UserErrorCode.JWT_TOKEN_PARSE_FAILED, e);
    }
  }

  public Instant getAccessTokenExpiry(String token) {
    return getTokenExpiry(token, accessTokenVerifier, "access");
  }

  public Instant getRefreshTokenExpiry(String token) {
    return getTokenExpiry(token, refreshTokenVerifier, "refresh");
  }

  // 만료 검증
  private Instant getTokenExpiry(String token, JWSVerifier verifier, String type) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);

      // 서명 검증
      if (!signedJWT.verify(verifier)) {
        throw new BusinessException(UserErrorCode.JWT_TOKEN_INVALID);
      }
      // 만료시간
      Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();
      if (expiration == null) {
        throw new BusinessException(UserErrorCode.JWT_TOKEN_PARSE_FAILED);
      }
      return expiration.toInstant();
    } catch (JOSEException e) {
      throw new BusinessException(UserErrorCode.JWT_TOKEN_INVALID, e);
    } catch (ParseException e) {
      throw new BusinessException(UserErrorCode.JWT_TOKEN_PARSE_FAILED, e);
    }
  }

  public String getUsernameFromToken(String token) {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      String subject = signedJWT.getJWTClaimsSet().getSubject();
      return subject;
    } catch (Exception e) {
      throw new BusinessException(UserErrorCode.JWT_TOKEN_INVALID, e);
    }
  }

  // Cookie 보호
  public ResponseCookie generateRefreshTokenCookie(String refreshToken) {
    return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
        .httpOnly(true)
        .secure(true)
        .path("/")
        .sameSite("Lax")
        .maxAge(jwtProperties.refreshTokenExp() / 1000)
        .build();
  }

  public ResponseCookie generateRefreshTokenExpirationCookie() {
    return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
        .httpOnly(true)
        .secure(true)
        .path("/")
        .sameSite("Lax")
        .maxAge(0)
        .build();
  }

  public void addRefreshCookie(HttpServletResponse response, String refreshToken) {
    ResponseCookie cookie = generateRefreshTokenCookie(refreshToken);
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  public void expireRefreshCookie(HttpServletResponse response) {
    ResponseCookie cookie = generateRefreshTokenExpirationCookie();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }
}
