package com.gitggal.clothesplz.repository.clothes;

import com.gitggal.clothesplz.entity.clothes.ClothesAttribute;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClothesAttributeRepository extends JpaRepository<ClothesAttribute, UUID> {

  @Query("""
      SELECT ca
      FROM ClothesAttribute ca
      JOIN FETCH ca.clothes
      JOIN FETCH ca.definition
      WHERE ca.clothes.id IN :clothesIds
      """)
  List<ClothesAttribute> findAllByClothesIdIn(
      @Param("clothesIds") List<UUID> clothesIds
  );

  @Modifying
  @Query("DELETE FROM ClothesAttribute ca WHERE ca.clothes.id = :clothesId")
  void deleteAllByClothesId(@Param("clothesId") UUID clothesId);

  void deleteAllByDefinitionId(@Param("definitionId") UUID definitionId);
}
