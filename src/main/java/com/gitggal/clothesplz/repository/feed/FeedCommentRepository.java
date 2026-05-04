package com.gitggal.clothesplz.repository.feed;

import com.gitggal.clothesplz.entity.feed.FeedComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FeedCommentRepository extends JpaRepository<FeedComment, UUID> {
}
