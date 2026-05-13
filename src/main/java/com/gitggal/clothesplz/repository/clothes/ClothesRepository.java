package com.gitggal.clothesplz.repository.clothes;

import com.gitggal.clothesplz.entity.clothes.Clothes;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClothesRepository extends JpaRepository<Clothes, UUID>, ClothesRepositoryCustom {

  boolean existsByIdAndOwnerId(UUID clothesId, UUID ownerId);

  List<Clothes> findByOwnerId(UUID ownerId);

}
