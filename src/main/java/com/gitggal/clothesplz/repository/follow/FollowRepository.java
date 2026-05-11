package com.gitggal.clothesplz.repository.follow;

import com.gitggal.clothesplz.entity.follow.Follow;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 팔로우 Repository 인터페이스
 */
public interface FollowRepository extends JpaRepository<Follow, UUID>, FollowRepositoryCustom {

  /**
   * 내가 이 사람을 팔로우하고 있는가를 조회
   */
  Optional<Follow> findByFollower_IdAndFollowee_Id(UUID followerId, UUID followeeId);

  /**
   * 내가 팔로우를 건 행의 수 (요약 조회 용)
   */
  long countByFollower_Id(UUID followerId);

  /**
   * 나를 팔로우하고 있는 행의 수 (요약 조회 용)
   */
  long countByFollowee_Id(UUID followeeId);

  /**
   * 팔로우 존재 여부 확인
   */
  boolean existsByFollower_IdAndFollowee_Id(UUID followerId, UUID followeeId);
}
