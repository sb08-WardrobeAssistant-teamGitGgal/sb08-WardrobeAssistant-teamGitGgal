package com.gitggal.clothesplz.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.ErrorResponse;
import com.gitggal.clothesplz.exception.code.ErrorCode;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.security.ClothesUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider tokenProvider;
  private final ClothesUserDetailsService userDetailsService;
  private final ObjectMapper objectMapper;
  private final JwtRegistry jwtRegistry;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    try {
      String token = resolveToken(request);

      if (StringUtils.hasText(token)) {
        if (!tokenProvider.validateAccessToken(token)) {
          throw new BusinessException(UserErrorCode.JWT_TOKEN_INVALID);
        }

        if (!jwtRegistry.hasActiveJwtInformationByAccessToken(token)) {
          throw new BusinessException(UserErrorCode.JWT_TOKEN_INVALID);
        }

        UUID userId = tokenProvider.getUserId(token);
        UserDetails userDetails = userDetailsService.loadUserById(userId);

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
      filterChain.doFilter(request, response);
    } catch (BusinessException e) {
      SecurityContextHolder.clearContext();
      sendErrorResponse(response, e.getErrorCode());
      return;

    } catch (Exception e) {
      SecurityContextHolder.clearContext();
      sendErrorResponse(response, UserErrorCode.JWT_TOKEN_INVALID);
      return;
    }
  }

  private String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }

  private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode)
      throws IOException {
    response.setStatus(errorCode.getHttpStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    ErrorResponse errorResponse = ErrorResponse.of(errorCode);
    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
  }
}
