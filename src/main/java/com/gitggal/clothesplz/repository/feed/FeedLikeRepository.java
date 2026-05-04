package com.gitggal.clothesplz.repository.feed;

import com.gitggal.clothesplz.entity.feed.FeedLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FeedLikeRepository extends JpaRepository<FeedLike, UUID> {
}
