package com.gitggal.clothesplz.entity.feed;

import com.gitggal.clothesplz.entity.base.BaseEntity;
import com.gitggal.clothesplz.entity.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "feed_likes", uniqueConstraints = {
    @UniqueConstraint(
        name = "UQ_feed_likes_feed_user",
        columnNames = {"feed_id", "user_id"}
    )
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedLike extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "feed_id", nullable = false, foreignKey = @ForeignKey(name = "FK_feeds_TO_feed_likes_1"), referencedColumnName = "id")
  private Feed feed;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "FK_users_TO_feed_likes_1"), referencedColumnName = "id")
  private User user;

  public FeedLike(Feed feed, User user) {
    this.feed = feed;
    this.user = user;
  }
}
