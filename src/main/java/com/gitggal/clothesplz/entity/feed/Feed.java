package com.gitggal.clothesplz.entity.feed;

import com.gitggal.clothesplz.dto.clothes.OotdDto;
import com.gitggal.clothesplz.entity.base.BaseUpdatableEntity;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.weather.Weather;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "feeds")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feed extends BaseUpdatableEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "weather_id", nullable = false, foreignKey = @ForeignKey(name = "FK_weathers_TO_feeds_1"), referencedColumnName = "id")
  private Weather weather;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_id", nullable = false, foreignKey = @ForeignKey(name = "FK_users_TO_feeds_1"), referencedColumnName = "id")
  private User author;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "ootds", nullable = false, columnDefinition = "JSONB")
  private List<OotdDto> ootds = new ArrayList<>();

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "like_count", nullable = false)
  private Long likeCount;

  @Column(name = "comment_count", nullable = false)
  private Long commentCount;

  @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<FeedLike> feedLikes = new ArrayList<>();

  @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<FeedComment> feedComments = new ArrayList<>();

  public Feed(Weather weather, User author, List<OotdDto> ootds, String content) {
    this.weather = weather;
    this.author = author;
    this.ootds = ootds;
    this.content = content;
    this.likeCount = 0L;
    this.commentCount = 0L;
  }

  public void update(String newContent) {
    if (newContent != null && !newContent.equals(this.content)) {
      this.content = newContent;
    }
  }

  public void increaseLikeCount() {
    this.likeCount++;
  }

  public void decreaseLikeCount() {
    if (this.likeCount > 0) {
      this.likeCount--;
    }
  }

  public void increaseCommentCount() {
    this.commentCount++;
  }
}
