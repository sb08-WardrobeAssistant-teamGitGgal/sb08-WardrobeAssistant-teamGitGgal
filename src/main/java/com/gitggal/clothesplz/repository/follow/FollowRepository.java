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
   * 내가 팔로우를 건 행의 수
   */
  long countByFollower_Id(UUID followerId);

  /**
   * 나를 팔로우하고 있는 행의 수
   */
  long countByFollowee_Id(UUID followeeId);
}
