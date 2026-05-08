package com.gitggal.clothesplz.service.follow.impl;

import com.gitggal.clothesplz.dto.follow.FollowDto;
import com.gitggal.clothesplz.dto.follow.FollowListResponse;
import com.gitggal.clothesplz.dto.follow.FollowSummaryDto;
import com.gitggal.clothesplz.entity.base.BaseEntity;
import com.gitggal.clothesplz.entity.follow.Follow;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.FollowErrorCode;
import com.gitggal.clothesplz.mapper.follow.FollowMapper;
import com.gitggal.clothesplz.repository.follow.FollowRepository;
import com.gitggal.clothesplz.service.follow.FollowService;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowServiceImpl implements FollowService {

  private static final String SORT_BY = "createdAt";
  private static final String SORT_DIRECTION = "DESCENDING";
  private final FollowRepository followRepository;
  private final FollowMapper followMapper;

  /**
   * 팔로잉(우) 목록 - 내가 팔로우 하는 목록
   *
   * @param followerId - follower ID
   * @param nameLike   - 이름 검색 필터
   * @param cursor     - 이전 페이지 마지막 createdAt
   * @param idAfter    - 이전 페이지 마지막 follow ID
   * @param limit      - 한 페이지 크기
   */
  @Override
  public FollowListResponse getFollowings(
      UUID followerId,
      String nameLike,
      String cursor,
      UUID idAfter,
      int limit) {

    log.info("[Service] 팔로잉(우) 목록 조회 요청 시작: followerId={}", followerId);

    Instant cursorInstant = parseCursor(cursor, idAfter);

    List<Follow> followings =
        followRepository.findFollowings(followerId, nameLike, cursorInstant, idAfter, limit + 1);

    return getFollowListResponse(followerId, null, nameLike, limit, followings);
  }

  /**
   * 팔로워 목록 - 나를 팔로우하는 목록
   *
   * @param followeeId - followeeId
   * @param nameLike   - 이름 검색 필터
   * @param cursor     - 이전 페이지 마지막 createdAt
   * @param idAfter    - 이전 페이지 마지막 follow ID
   * @param limit      - 한 페이지 크기
   */
  @Override
  public FollowListResponse getFollowers(
      UUID followeeId,
      String nameLike,
      String cursor,
      UUID idAfter,
      int limit) {

    log.info("[Service] 팔로워 목록 조회 요청 시작: followeeId={}", followeeId);

    Instant cursorInstant = parseCursor(cursor, idAfter);

    List<Follow> followers =
        followRepository.findFollowers(followeeId, nameLike, cursorInstant, idAfter, limit + 1);

    return getFollowListResponse(null, followeeId, nameLike, limit, followers);
  }

  /**
   * 팔로우 요약 조회
   */
  @Override
  public FollowSummaryDto getFollowSummary(UUID userId, UUID requesterId) {

    log.info("[Service] 팔로우 요약 조회 요청 시작: userId={}", userId);

    if (requesterId == null) return null;

    // 나를 팔로우하고 있는 사람 수
    long followerCount = followRepository.countByFollowee_Id(userId);

    // 내가 팔로우한 사람 수
    long followingCount = followRepository.countByFollower_Id(userId);

    // 현재 로그인 사용자가 이 사람을 팔로우 하고 있는가
    Optional<Follow> myFollow = followRepository.findByFollower_IdAndFollowee_Id(
        requesterId, userId);

    boolean followedByMe = myFollow.isPresent();

    UUID followedByMeId = myFollow.map(BaseEntity::getId).orElse(null);

    // 이 사람이 현재 로그인 사용자를 팔로우 하고 있는가
    boolean followingMe = followRepository.existsByFollower_IdAndFollowee_Id(userId, requesterId);

    log.info("[Service] 팔로우 요약 조회 요청 완료: userId={}", userId);

    return new FollowSummaryDto(
        userId,
        followerCount,
        followingCount,
        followedByMe,
        followedByMeId,
        followingMe
    );
  }

  /**
   * 공통 팔로우 리스트 메서드 (다음 페이지 여부 및 카운트 확인)
   */
  private @NonNull FollowListResponse getFollowListResponse(
      UUID followerId,
      UUID followeeId,
      String nameLike,
      int limit,
      List<Follow> followList) {

    boolean hasNext = followList.size() > limit;

    List<Follow> pageData = hasNext
        ? followList.subList(0, limit)
        : followList;

    String nextCursor = null;

    UUID nextIdAfter = null;

    // hasNext true이면 리스트가 비어있을 수가 없어서
    // 기존의 !pageData.isEmpty() 제거
    if (hasNext) {
      Follow last = pageData.get(pageData.size() - 1);

      nextCursor = last.getCreatedAt().toString();

      nextIdAfter = last.getId();
    }

    long totalCount = (followerId == null)
        ? followRepository.countFollowers(followeeId, nameLike)
        : followRepository.countFollowings(followerId, nameLike);

    List<FollowDto> dtoList = pageData.stream()
        .map(followMapper::toDto)
        .toList();

    if (followerId == null) {
      log.info("[Service] 팔로워 목록 조회 요청 완료: followeeId={}", followeeId);
    } else {
      log.info("[Service] 팔로잉(우) 목록 조회 요청 완료: followerId={}", followerId);
    }

    return new FollowListResponse(
        dtoList,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        SORT_BY,
        SORT_DIRECTION
    );
  }

  /**
   * 커서 검증 및 파싱 메서드
   */
  private Instant parseCursor(String cursor, UUID idAfter) {

    if ((cursor == null) != (idAfter == null)) {
      throw new BusinessException(FollowErrorCode.INVALID_CURSOR_FORMAT);
    }

    if (cursor == null) {
      return null;
    }

    try {
      return Instant.parse(cursor);
    } catch (DateTimeParseException e) {
      throw new BusinessException((FollowErrorCode.INVALID_CURSOR_FORMAT));
    }
  }
}
