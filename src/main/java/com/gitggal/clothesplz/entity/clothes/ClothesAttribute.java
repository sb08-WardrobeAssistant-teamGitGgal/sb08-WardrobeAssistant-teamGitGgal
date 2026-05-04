package com.gitggal.clothesplz.entity.clothes;

import com.gitggal.clothesplz.entity.base.BaseUpdatableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clothes_attribute")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClothesAttribute extends BaseUpdatableEntity {
}
