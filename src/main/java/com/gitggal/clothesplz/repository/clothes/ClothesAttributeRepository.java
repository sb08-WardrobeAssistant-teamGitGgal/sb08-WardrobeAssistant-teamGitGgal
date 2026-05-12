package com.gitggal.clothesplz.repository.clothes;

import com.gitggal.clothesplz.entity.clothes.ClothesAttribute;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClothesAttributeRepository extends JpaRepository<ClothesAttribute, UUID>, ClothesAttributeRepositoryCustom {

  @Query("SELECT ca FROM ClothesAttribute ca JOIN FETCH ca.clothes JOIN FETCH ca.definition WHERE ca.clothes.id IN :clothesIds")
  List<ClothesAttribute> findAllWithDefinitionByClothesIdIn(@Param("clothesIds") Collection<UUID> clothesIds);
}
