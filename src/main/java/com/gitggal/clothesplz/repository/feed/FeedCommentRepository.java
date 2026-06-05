package com.gitggal.clothesplz.repository.feed;

import com.gitggal.clothesplz.entity.feed.FeedComment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedCommentRepository extends JpaRepository<FeedComment, UUID> ,
    FeedCommentRepositoryCustom {

  @Modifying
  @Query("DELETE FROM FeedComment fc WHERE fc.feed.id = :feedId")
  void deleteAllByFeedId(@Param("feedId") UUID feedId);
}
