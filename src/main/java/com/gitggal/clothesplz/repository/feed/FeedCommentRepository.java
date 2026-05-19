package com.gitggal.clothesplz.repository.feed;

import com.gitggal.clothesplz.entity.feed.FeedComment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedCommentRepository extends JpaRepository<FeedComment, UUID> ,
    FeedCommentRepositoryCustom {
}
