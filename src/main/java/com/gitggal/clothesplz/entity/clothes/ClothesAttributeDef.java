package com.gitggal.clothesplz.entity.clothes;

import com.gitggal.clothesplz.entity.base.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "clothes_attribute_def")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ClothesAttributeDef extends BaseUpdatableEntity {

  @Column(name = "name", nullable = false)
  private String name;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "selectable_values", columnDefinition = "jsonb", nullable = false)
  private List<String> selectableValues;
}
