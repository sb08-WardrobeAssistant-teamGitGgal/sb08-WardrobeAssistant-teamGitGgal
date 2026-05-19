package com.gitggal.clothesplz.repository.feed;

import com.gitggal.clothesplz.entity.feed.Feed;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

public interface FeedRepository extends JpaRepository<Feed, UUID>, FeedRepositoryCustom {

  @EntityGraph(attributePaths = {"weather", "author"})
  Optional<Feed> findWithDetailsById(UUID feedId);

  @EntityGraph(attributePaths = {"weather", "author"})
  List<Feed> findAll();

  // 동시성 작업(좋아요, 댓글)을 위한 비관적 락 획득
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  // 3초동안 락 획득하지 못하면 타임아웃 발생
  @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
  Optional<Feed> findWithLockById(UUID feedId);

  boolean existsByIdAndAuthorId(UUID feedId, UUID authorId);
}
