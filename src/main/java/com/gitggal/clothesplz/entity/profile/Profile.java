package com.gitggal.clothesplz.entity.profile;

import com.gitggal.clothesplz.entity.base.BaseUpdatableEntity;
import com.gitggal.clothesplz.entity.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Profile extends BaseUpdatableEntity {

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "image_url")
  private String imageUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "gender", length = 10)
  private Gender gender;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(name = "latitude")
  private Double latitude;

  @Column(name = "longitude")
  private Double longitude;

  @Column(name = "temp_sensitivity")
  @Builder.Default
  private Short tempSensitivity = 3;

  @Column(name = "grid_x")
  private Integer gridX;

  @Column(name = "grid_y")
  private Integer gridY;

  public void update(
      Gender gender,
      String imageUrl,
      LocalDate birthDate,
      Double latitude,
      Double longitude,
      Integer gridX,
      Integer gridY,
      Integer tempSensitivity
  ) {
    if (gender != null) {
      this.gender = gender;
    }

    if (imageUrl != null) {
      this.imageUrl = imageUrl;
    }

    if (birthDate != null) {
      this.birthDate = birthDate;
    }

    boolean hasAnyLocationValue =
        gridX != null || gridY != null || latitude != null || longitude != null;
    boolean hasAllLocationValues =
        gridX != null && gridY != null && latitude != null && longitude != null;

    if (hasAllLocationValues) {
      this.latitude = latitude;
      this.longitude = longitude;
      this.gridX = gridX;
      this.gridY = gridY;
    } else if (hasAnyLocationValue) {
      throw new IllegalArgumentException(
          "위치 정보는 부분 수정할 수 없습니다. latitude, longitude, gridX, gridY를 모두 함께 전달해야 합니다.");
    }

    if (tempSensitivity != null) {
      this.tempSensitivity = tempSensitivity.shortValue();
    }
  }
}
