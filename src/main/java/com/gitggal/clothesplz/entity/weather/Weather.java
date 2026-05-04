package com.gitggal.clothesplz.entity.weather;

import com.gitggal.clothesplz.entity.base.BaseUpdatableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "weathers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Weather extends BaseUpdatableEntity {
}
