package com.gitggal.clothesplz.controller.follow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.config.TestSecurityConfig;
import com.gitggal.clothesplz.dto.follow.FollowDto;
import com.gitggal.clothesplz.dto.follow.FollowListResponse;
import com.gitggal.clothesplz.dto.follow.FollowSummaryDto;
import com.gitggal.clothesplz.dto.follow.UserSummary;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.GlobalExceptionHandler;
import com.gitggal.clothesplz.exception.code.FollowErrorCode;
import com.gitggal.clothesplz.security.jwt.JwtAuthenticationFilter;
import com.gitggal.clothesplz.service.follow.FollowService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// excludeFilters: JwtAuthenticationFilter 제외 (JWT 검증 불필요)
@Import({
    GlobalExceptionHandler.class,
    TestSecurityConfig.class
})
@WebMvcTest(
    controllers = FollowController.class,
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class
        )
    }
)
@DisplayName("팔로우 컨트롤러 테스트")
public class FollowControllerTest {

  // 실제 서버 없이 컨트롤러 테스트 용도 (가짜 HTTP 요청)
  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private FollowService followService;

  // 팔로우 생성
  @Test
  @DisplayName("팔로우 생성 성공 시 201 Created와 FollowDto를 반환한다.")
  void createFollow_returns_201_and_FollowDto() throws Exception {

    // given
    UUID followerId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();

    UUID followId = UUID.randomUUID();

    FollowDto responseDto = new FollowDto(
        followId,
        new UserSummary(followerId, "follower", null),
        new UserSummary(followeeId, "followee", null)
    );

    given(followService.createFollow(any()))
        .willReturn(responseDto);

    String requestBody = objectMapper.writeValueAsString(
        Map.of("followerId", followerId, "followeeId", followeeId)
    );

    // when & then
    mockMvc.perform(post("/api/follows")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
            .with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(followId.toString()))
        .andExpect(jsonPath("$.follower.name").value("follower"))
        .andExpect(jsonPath("$.followee.name").value("followee"));
  }

  @Test
  @DisplayName("자기 자신을 팔로우하면 400 Bad Request를 반환한다.")
  void createFollow_selfFollow_returns400() throws Exception {

    // given
    // willThrow: 해당 메서드가 호출될 때 예외를 던지도록 설정
    willThrow(new BusinessException(FollowErrorCode.SELF_FOLLOW_NOT_ALLOWED))
        .given(followService).createFollow(any());

    UUID sameId = UUID.randomUUID();

    String requestBody = objectMapper.writeValueAsString(
        Map.of("followerId", sameId, "followeeId", sameId)
    );

    // when & then
    mockMvc.perform(post("/api/follows")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
            .with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("이미 팔로우 중이면 409 Conflict를 반환한다.")
  void createFollow_alreadyExists_returns409() throws Exception {

    // given
    willThrow(new BusinessException(FollowErrorCode.FOLLOW_ALREADY_EXISTS))
        .given(followService).createFollow(any());

    String requestBody = objectMapper.writeValueAsString(
        Map.of("followerId", UUID.randomUUID(), "followeeId", UUID.randomUUID())
    );

    // when & then
    mockMvc.perform(post("/api/follows")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
            .with(csrf()))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("followerId가 null인 요청은 400 Bad Request를 반환한다.")
  void createFollow_nullFollowerId_returns400() throws Exception {

    // given: followerId를 빈 바디로 전송 (null)
    String requestBody = objectMapper.writeValueAsString(
        Map.of("followeeId", UUID.randomUUID())
    );

    // when & then
    mockMvc.perform(post("/api/follows")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
            .with(csrf()))
        .andExpect(status().isBadRequest());
  }

  // 팔로우 취소
  @Test
  @DisplayName("팔로우 취소 성공 시 204 No Content를 반환한다.")
  void cancelFollow_validRequest_returns204() throws Exception {

    // given
    UUID followId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/follows/{followId}", followId)
            .with(csrf()))
        .andExpect(status().isNoContent());

    then(followService).should().cancelFollow(followId);
  }

  @Test
  @DisplayName("존재하지 않는 followId로 취소하면 404 Not Found를 반환한다.")
  void cancelFollow_notFound_returns404() throws Exception {

    // given
    UUID followId = UUID.randomUUID();

    willThrow(new BusinessException(FollowErrorCode.FOLLOW_NOT_FOUND))
        .given(followService).cancelFollow(followId);

    // when & then
    mockMvc.perform(delete("/api/follows/{followId}", followId)
            .with(csrf()))
        .andExpect(status().isNotFound());
  }

  // 팔로잉 목록 조회
  @Test
  @DisplayName("팔로잉 목록 조회 성공 시 200 OK와 응답 데이터를 반환한다.")
  void getFollowings_validRequest_returns200() throws Exception {

    // given
    UUID followerId = UUID.randomUUID();

    FollowListResponse response = new FollowListResponse(
        List.of(), null, null, false, 0L, "createdAt", "DESCENDING");

    given(followService.getFollowings(
        any(UUID.class), isNull(), isNull(), isNull(), anyInt()))
        .willReturn(response);

    // when & then
    mockMvc.perform(get("/api/follows/followings")
            .param("followerId", followerId.toString())
            .param("limit", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalCount").value(0))
        .andExpect(jsonPath("$.sortBy").value("createdAt"));
  }

  @Test
  @DisplayName("followerId 파라미터 없이 요청하면 400을 반환한다.")
  void getFollowings_missingFollowerId_returns400() throws Exception {

    // when & then: followerId 없이 limit만 전송
    mockMvc.perform(get("/api/follows/followings")
            .param("limit", "10"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("limit 파라미터 없이 요청하면 400을 반환한다.")
  void getFollowings_missingLimit_returns400() throws Exception {

    // when & then: limit 없이 followerId만 전송
    mockMvc.perform(get("/api/follows/followings")
            .param("followerId", UUID.randomUUID().toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("cursor와 idAfter가 모두 있을 때 서비스에 올바르게 전달된다.")
  void getFollowings_withCursorAndIdAfter_passesToService() throws Exception {

    // given
    UUID followerId = UUID.randomUUID();
    String cursor = "2024-06-01T00:00:00Z";
    UUID idAfter = UUID.randomUUID();

    FollowListResponse response = new FollowListResponse(
        List.of(), null, null, false, 0L, "createdAt", "DESCENDING");

    given(followService.getFollowings(
        eq(followerId), isNull(), eq(cursor), eq(idAfter), anyInt()))
        .willReturn(response);

    // when & then
    mockMvc.perform(get("/api/follows/followings")
            .param("followerId", followerId.toString())
            .param("cursor", cursor)
            .param("idAfter", idAfter.toString())
            .param("limit", "10"))
        .andExpect(status().isOk());
  }

  // 팔로워 목록 조회
  @Test
  @DisplayName("팔로워 목록 조회 성공 시 200 OK와 응답 데이터를 반환한다.")
  void getFollowers_validRequest_returns200() throws Exception {

    // given
    UUID followeeId = UUID.randomUUID();

    FollowListResponse response = new FollowListResponse(
        List.of(), null, null, false, 3L, "createdAt", "DESCENDING");

    given(followService.getFollowers(
        any(UUID.class), isNull(), isNull(), isNull(), anyInt()))
        .willReturn(response);

    // when & then
    mockMvc.perform(get("/api/follows/followers")
        .param("followeeId", followeeId.toString())
        .param("limit", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasNext").value(false))
        .andExpect(jsonPath("$.totalCount").value(3));
  }

  @Test
  @DisplayName("커서 형식이 잘못된 요청이면 400을 반환한다.")
  void getFollowers_invalidCursor_returns400() throws Exception {

    // given
    willThrow(new BusinessException(FollowErrorCode.INVALID_CURSOR_FORMAT))
        .given(followService).getFollowers(any(), any(), anyString(), any(), anyInt());

    // when & then
    mockMvc.perform(get("/api/follows/followers")
            .param("followeeId", UUID.randomUUID().toString())
            .param("cursor", "invalid")
            .param("idAfter", UUID.randomUUID().toString())
            .param("limit", "10"))
        .andExpect(status().isBadRequest());
  }

  // 팔로우 요약 조회
  @Test
  @DisplayName("팔로우 요약 조회 성공 시 200 OK와 FollowSummaryDto를 반환한다.")
  void getFollowSummary_validRequest_returns200() throws Exception {

    // given
    UUID userId = UUID.randomUUID();
    UUID requesterId = UUID.randomUUID();

    FollowSummaryDto dto = new FollowSummaryDto(
        userId, 10L, 5L, true, UUID.randomUUID(), false
    );

    given(followService.getFollowSummary(userId, requesterId))
        .willReturn(dto);

    // when & then
    mockMvc.perform(get("/api/follows/summary")
            .param("userId", userId.toString())
            .param("requesterId", requesterId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.followerCount").value(10))
        .andExpect(jsonPath("$.followingCount").value(5))
        .andExpect(jsonPath("$.followedByMe").value(true));
  }

  @Test
  @DisplayName("userId 파라미터 없이 요청하면 400을 반환한다")
  void getFollowSummary_missingUserId_returns400() throws Exception {

    // when & then: userId 없이 요청
    mockMvc.perform(get("/api/follows/summary")
            .param("requesterId", UUID.randomUUID().toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("requesterId 없이 요청하면 서비스에 null이 전달되어 200을 반환한다.")
  void getFollowSummary_withoutRequesterId_passes200() throws Exception {

    // given: requesterId=null → 서비스가 null 반환 (비로그인 사용자)
    UUID userId = UUID.randomUUID();

    given(followService.getFollowSummary(eq(userId), isNull()))
        .willReturn(null);

    // when & then
    mockMvc.perform(get("/api/follows/summary")
            .param("userId", userId.toString()))
        .andExpect(status().isOk());
  }
}
