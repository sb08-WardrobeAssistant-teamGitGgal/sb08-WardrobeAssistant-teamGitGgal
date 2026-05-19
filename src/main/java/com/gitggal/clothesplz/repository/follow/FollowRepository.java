package com.gitggal.clothesplz.repository.follow;

import com.gitggal.clothesplz.entity.follow.Follow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  /**
   * 피드 등록 알림용: 특정 사람을 팔로우하는 모든 follower ID 조회
   * 피드를 등록한 사람의 팔로워들에게 알림을 보내야 한다.
   */
  @Query("SELECT f.follower.id FROM Follow f WHERE f.followee.id = :followeeId")
  List<UUID> findFollowerIdsByFolloweeId(@Param("followeeId") UUID followeeId);
}
