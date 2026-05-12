package com.gitggal.clothesplz.service.follow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.gitggal.clothesplz.dto.follow.FollowCreateRequest;
import com.gitggal.clothesplz.dto.follow.FollowDto;
import com.gitggal.clothesplz.dto.follow.FollowListResponse;
import com.gitggal.clothesplz.dto.follow.FollowSummaryDto;
import com.gitggal.clothesplz.dto.notification.NotificationRequest;
import com.gitggal.clothesplz.entity.follow.Follow;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.FollowErrorCode;
import com.gitggal.clothesplz.mapper.follow.FollowMapper;
import com.gitggal.clothesplz.repository.follow.FollowRepository;
import com.gitggal.clothesplz.repository.user.UserRepository;
import com.gitggal.clothesplz.service.follow.impl.FollowServiceImpl;
import com.gitggal.clothesplz.service.notification.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("팔로우 서비스 테스트")
public class FollowServiceTest {

  @Mock
  private FollowRepository followRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private FollowMapper followMapper;

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private FollowServiceImpl followService;

  // 팔로우 생성 테스트
  @Test
  @DisplayName("팔로우 생성 요청 성공 시 Follow를 저장하고 알림을 발송한다.")
  void createFollow_successAndSendNotification() {

    // given
    UUID followerId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();
    FollowCreateRequest request = new FollowCreateRequest(followerId, followeeId);

    // 중복 팔로우 X
    given(followRepository
        .existsByFollower_IdAndFollowee_Id(followerId, followeeId))
        .willReturn(false);

    User mockFollower = mock(User.class);
    User mockFollowee = mock(User.class);
    given(mockFollower.getName()).willReturn("팔로워A");

    given(userRepository.getReferenceById(followerId)).willReturn(mockFollower);
    given(userRepository.getReferenceById(followeeId)).willReturn(mockFollowee);

    // savedFollow는 follower 정보가 있어야 알림 발송 시 이름을 가져올 수 있음
    Follow savedFollow = mock(Follow.class);
    given(savedFollow.getFollower()).willReturn(mockFollower);
    given(followRepository.save(any(Follow.class))).willReturn(savedFollow);

    FollowDto followDto = mock(FollowDto.class);
    given(followMapper.toDto(savedFollow)).willReturn(followDto);

    // when
    FollowDto result = followService.createFollow(request);

    // then
    assertThat(result).isEqualTo(followDto);

    // followRepository.save()가 정확히 호출되었는지 확인
    then(followRepository).should().save(any(Follow.class));

    // notificationService.send()가 알림 요청과 함께 호출되었는지 확인
    then(notificationService).should().send(any(NotificationRequest.class));
  }

  @Test
  @DisplayName("자기 자신을 팔로우하면 SELF_FOLLOW_NOT_ALLOWED 예외가 발생한다.")
  void createFollow_selfFollow_throwsException() {

    // given
    UUID sameId = UUID.randomUUID();
    FollowCreateRequest request = new FollowCreateRequest(sameId, sameId);

    // when & then
    assertThatThrownBy(() -> followService.createFollow(request))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex ->
            assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(FollowErrorCode.SELF_FOLLOW_NOT_ALLOWED)
        );

    // then
    then(followRepository).should(never()).save(any());
  }

  @Test
  @DisplayName("이미 팔로우 중인 경우 FOLLOW_ALREADY_EXISTS 예외가 발생한다.")
  void createFollow_alreadyExists_throwsException() {

    // given
    UUID followerId = UUID.randomUUID();
    UUID followeeId = UUID.randomUUID();
    FollowCreateRequest request = new FollowCreateRequest(followerId, followeeId);

    // 이미 팔로우 중이라는 Mock 설정 (true 반환)
    given(followRepository.existsByFollower_IdAndFollowee_Id(followerId, followeeId))
        .willReturn(true);

    // when & then
    assertThatThrownBy(() -> followService.createFollow(request))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex ->
            assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(FollowErrorCode.FOLLOW_ALREADY_EXISTS)
        );

    then(followRepository).should(never()).save(any());
  }

  // 팔로우 취소 테스트
  @Test
  @DisplayName("존재하는 followId로 취소하면 정상적으로 취소(삭제)된다.")
  void cancelFollow_success() {

    // given
    UUID followId = UUID.randomUUID();
    Follow follow = mock(Follow.class);

    given(followRepository.findById(followId))
        .willReturn(Optional.of(follow));

    // when
    followService.cancelFollow(followId);

    // then
    then(followRepository).should().delete(follow);
  }

  @Test
  @DisplayName("존재하지 않는 followId로 취소하면 FOLLOW_NOT_FOUND 예외가 발생한다.")
  void cancelFollow_notFound_throws() {

    // given
    UUID followId = UUID.randomUUID();

    given(followRepository.findById(followId))
        .willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> followService.cancelFollow(followId))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex ->
            assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(FollowErrorCode.FOLLOW_NOT_FOUND)
        );

    // then
    then(followRepository).should(never()).delete(any());
  }

  // 팔로잉 목록 조회
  @Test
  @DisplayName("다음 페이지가 있을 때 hasNext=true와 nextCursor를 반환한다.")
  void getFollowings_hasNextTrue_returnsNextCursor() {

    // given
    UUID followerId = UUID.randomUUID();
    int limit = 2;

    // follow2가 마지막 페이지 항목
    Follow follow1 = mock(Follow.class);
    Follow follow2 = mock(Follow.class);
    Follow follow3 = mock(Follow.class);

    UUID lastId = UUID.randomUUID();
    Instant lastCreatedAt = Instant.now();

    given(follow2.getId()).willReturn(lastId);
    given(follow2.getCreatedAt()).willReturn(lastCreatedAt);

    given(followRepository.findFollowings(
        followerId, null, null, null, limit + 1))
        .willReturn(List.of(follow1, follow2, follow3));

    given(followRepository.countFollowings(followerId, null))
        .willReturn(10L);

    given(followMapper.toDto(any(Follow.class)))
        .willReturn(mock(FollowDto.class));

    FollowListResponse response = followService
        .getFollowings(followerId, null, null, null, limit);

    // then
    assertThat(response.hasNext()).isTrue();
    assertThat(response.data()).hasSize(limit);

    assertThat(response.nextCursor()).isEqualTo(lastCreatedAt.toString());
    assertThat(response.nextIdAfter()).isEqualTo(lastId);
  }

  @Test
  @DisplayName("마지막 페이지이면 hasNext=false이고 nextCursor는 null이다.")
  void getFollowings_lastPage_hasNextFalse() {

    // given
    UUID followerId = UUID.randomUUID();
    int limit = 5;

    // limit+1(6개) 요청 시 1개만 반환 → hasNext=false
    given(followRepository.findFollowings(
        followerId, null, null, null, limit + 1))
        .willReturn(List.of(mock(Follow.class)));

    given(followRepository.countFollowings(followerId, null))
        .willReturn(1L);

    given(followMapper.toDto(any(Follow.class)))
        .willReturn(mock(FollowDto.class));

    // when
    FollowListResponse response = followService.getFollowings(
        followerId, null, null, null, limit);

    // then
    assertThat(response.hasNext()).isFalse();
    assertThat(response.nextCursor()).isNull();
    assertThat(response.totalCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("cursor만 있고 idAfter가 null이면 INVALID_CURSOR_FORMAT 예외가 발생한다.")
  void getFollowings_cursorWithoutIdAfter_throws() {

    // given
    UUID followerId = UUID.randomUUID();
    String cursor = "2024-01-01T00:00:00Z";

    // when & then
    assertThatThrownBy(() ->
        followService.getFollowings(followerId, null, cursor, null, 10))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex ->
            assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(FollowErrorCode.INVALID_CURSOR_FORMAT)
        );
  }

  // 팔로워 목록 조회
  @Test
  @DisplayName("팔로워 목록 조회 시 FollowListResponse를 정상 반환한다.")
  void getFollowers_returnsFollowListResponse() {

    // given
    UUID followeeId = UUID.randomUUID();
    int limit = 5;

    // 2개 반환 (limit+1=6보다 적음 -> hasNext=false)
    given(followRepository.findFollowers(
        followeeId, null, null, null, limit + 1))
        .willReturn(List.of(mock(Follow.class), mock(Follow.class)));

    given(followRepository.countFollowers(followeeId, null))
        .willReturn(2L);

    given(followMapper.toDto(any(Follow.class)))
        .willReturn(mock(FollowDto.class));

    // when
    FollowListResponse response = followService.getFollowers(followeeId, null, null, null, limit);

    // then
    assertThat(response.hasNext()).isFalse();
    assertThat(response.data()).hasSize(2);
    assertThat(response.totalCount()).isEqualTo(2L);
    assertThat(response.sortBy()).isEqualTo("createdAt");
    assertThat(response.sortDirection()).isEqualTo("DESCENDING");
  }

  @Test
  @DisplayName("idAfter만 있고 cursor가 null이면 INVALID_CURSOR_FORMAT 예외가 발생한다.")
  void getFollowers_idAfterWithoutCursor_throws() {

    // given: cursor=null, idAfter=UUID
    UUID followeeId = UUID.randomUUID();
    UUID idAfter = UUID.randomUUID();

    // when & then
    assertThatThrownBy(() ->
        followService.getFollowers(followeeId, null, null, idAfter, 10))
        .isInstanceOf(BusinessException.class)
        .satisfies(ex ->
            assertThat(((BusinessException) ex).getErrorCode())
                .isEqualTo(FollowErrorCode.INVALID_CURSOR_FORMAT)
        );
  }

  // 팔로우 요약 조회 테스트
  @Test
  @DisplayName("requesterId가 null이면 null을 반환한다.")
  void getFollowSummary_nullRequesterId_returnsNull() {

    // given
    UUID userId = UUID.randomUUID();

    // when
    FollowSummaryDto result = followService.getFollowSummary(userId, null);

    // then: 로그인 X -> 요약 정보 null
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("requesterId가 있으면 팔로우 요약 정보를 정상 반환한다.")
  void getFollowSummary_withRequesterId_returnsDto() {

    // given
    UUID userId = UUID.randomUUID();
    UUID requesterId = UUID.randomUUID();

    // userId를 팔로우하는 사람 수 = 10
    given(followRepository.countByFollowee_Id(userId))
        .willReturn(10L);

    // userId가 팔로우하는 사람 수 = 5
    given(followRepository.countByFollower_Id(userId))
        .willReturn(5L);

    Follow myFollow = mock(Follow.class);
    UUID followId = UUID.randomUUID();

    given(myFollow.getId()).willReturn(followId);
    given(followRepository.findByFollower_IdAndFollowee_Id(requesterId, userId))
        .willReturn(Optional.of(myFollow));

    // userId → requester 팔로우 관계 없음
    given(followRepository.existsByFollower_IdAndFollowee_Id(userId, requesterId))
        .willReturn(false);

    // when
    FollowSummaryDto result = followService.getFollowSummary(userId, requesterId);

    // then
    assertThat(result).isNotNull();
    assertThat(result.followerCount()).isEqualTo(10L);
    assertThat(result.followingCount()).isEqualTo(5L);
    assertThat(result.followedByMe()).isTrue();              // 내가 팔로우 중
    assertThat(result.followedByMeId()).isEqualTo(followId); // 언팔로우용 ID
    assertThat(result.followingMe()).isFalse();              // 상대방은 나를 팔로우 X
  }

  @Test
  @DisplayName("내가 팔로우하지 않았을 때 followedByMe=false이고, followedByMeId=null이다.")
  void getFollowSummary_notFollowedByMe_followedByMeFalse() {
    // given
    UUID userId = UUID.randomUUID();
    UUID requesterId = UUID.randomUUID();

    given(followRepository.countByFollowee_Id(userId)).willReturn(0L);
    given(followRepository.countByFollower_Id(userId)).willReturn(0L);

    // 팔로우 관계 없음
    given(followRepository.findByFollower_IdAndFollowee_Id(requesterId, userId))
        .willReturn(Optional.empty());
    given(followRepository.existsByFollower_IdAndFollowee_Id(userId, requesterId))
        .willReturn(false);

    // when
    FollowSummaryDto result = followService.getFollowSummary(userId, requesterId);

    // then
    assertThat(result.followedByMe()).isFalse();
    assertThat(result.followedByMeId()).isNull();
  }

}
