package com.gitggal.clothesplz.repository.feed;

import com.gitggal.clothesplz.entity.feed.FeedLike;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedLikeRepository extends JpaRepository<FeedLike, UUID> {

  boolean existsByFeedIdAndUserId(UUID feedId, UUID userId);

  Optional<FeedLike> findByFeedIdAndUserId(UUID feedId, UUID userId);

  // 해당 유저가 실제로 좋아요 누른 feed ID만 set에 담아서 반환
  @Query("SELECT fl.feed.id FROM FeedLike fl WHERE fl.user.id = :userId AND fl.feed.id IN :feedIds")
  Set<UUID> findFeedIdsByUserId(@Param("userId") UUID userId, @Param("feedIds") List<UUID> feedIds);
}
