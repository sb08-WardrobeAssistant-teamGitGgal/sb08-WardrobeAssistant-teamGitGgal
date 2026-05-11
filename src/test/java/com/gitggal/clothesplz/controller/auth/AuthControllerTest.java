package com.gitggal.clothesplz.controller.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gitggal.clothesplz.config.TestSecurityConfig;
import com.gitggal.clothesplz.exception.GlobalExceptionHandler;
import com.gitggal.clothesplz.security.jwt.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;


@Import({
    GlobalExceptionHandler.class,
    TestSecurityConfig.class
})
@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class
        )
    }
)
@DisplayName("AuthController Test")
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Nested
  @DisplayName("CSRF 토큰 발급")
  class GetCsrfToken {

    @Test
    @DisplayName("성공 - CSRF 토큰 요청 시 204 No Content를 반환한다")
    void success_getCsrfToken() throws Exception {
      // when & then
      mockMvc.perform(get("/api/auth/csrf-token")
              .with(csrf()))
          .andDo(print())
          .andExpect(status().isNoContent());
    }
  }
}