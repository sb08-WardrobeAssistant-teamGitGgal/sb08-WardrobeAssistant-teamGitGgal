package com.gitggal.clothesplz.entity.clothes;

import com.gitggal.clothesplz.entity.base.BaseUpdatableEntity;
import com.gitggal.clothesplz.entity.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clothes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Clothes extends BaseUpdatableEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @Column(name = "name", length = 100, nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", length = 20, nullable = false)
  private ClothesType type;

  @Column(name = "image_url")
  private String imageUrl;

  @Column(name = "purchase_url")
  private String purchaseUrl;
}
