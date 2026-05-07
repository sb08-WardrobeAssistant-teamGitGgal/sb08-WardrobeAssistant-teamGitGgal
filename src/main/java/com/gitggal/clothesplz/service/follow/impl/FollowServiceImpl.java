package com.gitggal.clothesplz.service.follow.impl;

import com.gitggal.clothesplz.dto.follow.FollowDto;
import com.gitggal.clothesplz.dto.follow.FollowListResponse;
import com.gitggal.clothesplz.entity.follow.Follow;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.FollowErrorCode;
import com.gitggal.clothesplz.mapper.follow.FollowMapper;
import com.gitggal.clothesplz.repository.follow.FollowRepository;
import com.gitggal.clothesplz.service.follow.FollowService;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
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

    return getFollowListResponse(followerId, null, limit, followings);
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

    return getFollowListResponse(null, followeeId, limit, followers);
  }

  /**
   * 공통 팔로우 리스트 메서드 (다음 페이지 여부 및 카운트 확인)
   */
  private @NonNull FollowListResponse getFollowListResponse(
      UUID followerId,
      UUID followeeId,
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

    long totalCount = (followerId == null) ? followRepository.countByFollowee_Id(followeeId)
        : followRepository.countByFollower_Id(followerId);

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
