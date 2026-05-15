package com.gitggal.clothesplz.controller.message;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gitggal.clothesplz.config.TestSecurityConfig;
import com.gitggal.clothesplz.dto.message.DirectMessageDtoCursorResponse;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.user.UserRole;
import com.gitggal.clothesplz.exception.GlobalExceptionHandler;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import com.gitggal.clothesplz.security.jwt.JwtAuthenticationFilter;
import com.gitggal.clothesplz.service.message.DirectMessageService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@WebMvcTest(
    controllers = DirectMessageController.class,
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class
        )
    }
)
@DisplayName("DM 컨트롤러 테스트")
public class DirectMessageControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private DirectMessageService directMessageService;

  private ClothesUserDetails mockUserDetails(UUID userId) {
    UserDto userDto = new UserDto(userId, Instant.now(), "test@test.com", "테스터", UserRole.USER, false);
    return new ClothesUserDetails(userDto, "password");
  }

  @Test
  @DisplayName("DM 목록 조회 성공 시 200 OK 및 정상 응답 반환")
  void getMessages_returns_200() throws Exception {

    // given
    UUID userId = UUID.randomUUID();
    UUID partnerId = UUID.randomUUID();

    DirectMessageDtoCursorResponse response =
        new DirectMessageDtoCursorResponse(
        List.of(), null, null, false, 0L, "createdAt", "DESCENDING");

    given(directMessageService.getMessages(any(), any(), any(), any(), anyInt()))
        .willReturn(response);

    // when & then
    mockMvc.perform(get("/api/direct-messages")
            .param("userId", userId.toString())
            .with(user(mockUserDetails(userId)))
            .param("limit", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.sortBy").value("createdAt"));
  }
}
