package com.gitggal.clothesplz.repository.feed;

import com.gitggal.clothesplz.entity.feed.Feed;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;

public interface FeedRepository extends JpaRepository<Feed, UUID> {

  @EntityGraph(attributePaths = {"weather", "author"})
  Optional<Feed> findWithDetailsById(UUID feedId);

  // 동시성 작업(좋아요, 댓글)을 위한 비관적 락 획득
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Feed> findWithLockById(UUID feedId);
}
