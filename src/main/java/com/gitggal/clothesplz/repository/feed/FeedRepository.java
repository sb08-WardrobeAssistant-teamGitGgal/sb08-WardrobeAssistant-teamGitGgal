package com.gitggal.clothesplz.repository.feed;

import com.gitggal.clothesplz.entity.feed.Feed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FeedRepository extends JpaRepository<Feed, UUID> {
}
