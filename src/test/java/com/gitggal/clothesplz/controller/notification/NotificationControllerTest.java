package com.gitggal.clothesplz.controller.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gitggal.clothesplz.config.TestSecurityConfig;
import com.gitggal.clothesplz.dto.notification.NotificationDtoCursorResponse;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.GlobalExceptionHandler;
import com.gitggal.clothesplz.exception.code.NotificationErrorCode;
import com.gitggal.clothesplz.security.jwt.JwtAuthenticationFilter;
import com.gitggal.clothesplz.service.notification.NotificationService;
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

@Import({
    GlobalExceptionHandler.class,
    TestSecurityConfig.class
})
@WebMvcTest(
    controllers = NotificationController.class,
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class
        )
    }
)
@DisplayName("알림 컨트롤러 테스트")
class NotificationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private NotificationService notificationService;

  // ─── GET /api/notifications ────────────────────────────────────────────────

  @Test
  @DisplayName("알림 목록 조회 시 200 OK와 응답 데이터를 반환한다")
  void getNotifications_validRequest_returns200() throws Exception {
    // given
    UUID receiverId = UUID.randomUUID();
    NotificationDtoCursorResponse response = new NotificationDtoCursorResponse(
        List.of(), null, null, false, 5L, "createdAt", "DESCENDING");

    given(notificationService.getNotifications(any(UUID.class), isNull(), isNull(), anyInt()))
        .willReturn(response);

    // when & then
    mockMvc.perform(get("/api/notifications")
            .param("receiverId", receiverId.toString())
            .param("limit", "10")
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalCount").value(5));
  }

  @Test
  @DisplayName("limit 파라미터 없이 요청하면 400을 반환한다")
  void getNotifications_missingLimit_returns400() throws Exception {
    mockMvc.perform(get("/api/notifications")
            .param("receiverId", UUID.randomUUID().toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("receiverId 파라미터 없이 요청하면 400을 반환한다")
  void getNotifications_missingReceiverId_returns400() throws Exception {
    mockMvc.perform(get("/api/notifications")
            .param("limit", "10"))
        .andExpect(status().isBadRequest());
  }

  // ─── DELETE /api/notifications/{notificationId} ────────────────────────────

  @Test
  @DisplayName("알림 읽음 처리 시 204 No Content를 반환한다")
  void deleteNotification_validRequest_returns204() throws Exception {
    // given
    UUID notificationId = UUID.randomUUID();
    UUID requesterId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/notifications/{notificationId}", notificationId)
            .param("requesterId", requesterId.toString())
            .with(csrf()))
        .andExpect(status().isNoContent());

    then(notificationService).should().deleteNotification(notificationId, requesterId);
  }

  @Test
  @DisplayName("존재하지 않는 알림 삭제 시 404를 반환한다")
  void deleteNotification_notFound_returns404() throws Exception {
    // given
    UUID notificationId = UUID.randomUUID();
    UUID requesterId = UUID.randomUUID();

    willThrow(new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND))
        .given(notificationService).deleteNotification(notificationId, requesterId);

    // when & then
    mockMvc.perform(delete("/api/notifications/{notificationId}", notificationId)
            .param("requesterId", requesterId.toString())
            .with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("본인 알림이 아닌 경우 삭제 시 403을 반환한다")
  void deleteNotification_notOwner_returns403() throws Exception {
    // given
    UUID notificationId = UUID.randomUUID();
    UUID requesterId = UUID.randomUUID();

    willThrow(new BusinessException(NotificationErrorCode.UNAUTHORIZED_NOTIFICATION_ACCESS))
        .given(notificationService).deleteNotification(notificationId, requesterId);

    // when & then
    mockMvc.perform(delete("/api/notifications/{notificationId}", notificationId)
            .param("requesterId", requesterId.toString()))
        .andExpect(status().isForbidden());
  }
}
