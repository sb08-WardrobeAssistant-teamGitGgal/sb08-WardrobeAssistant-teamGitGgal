package com.gitggal.clothesplz.entity.feed;

import com.gitggal.clothesplz.entity.base.BaseEntity;
import com.gitggal.clothesplz.entity.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "feed_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedComment extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "feed_id", nullable = false, foreignKey = @ForeignKey(name = "FK_feeds_TO_feed_comments_1"), referencedColumnName = "id")
  private Feed feed;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_id", nullable = false, foreignKey = @ForeignKey(name = "FK_users_TO_feed_comments_1"), referencedColumnName = "id")
  private User author;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  public FeedComment(Feed feed, User author, String content) {
    this.feed = feed;
    this.author = author;
    this.content = content;
  }
}
