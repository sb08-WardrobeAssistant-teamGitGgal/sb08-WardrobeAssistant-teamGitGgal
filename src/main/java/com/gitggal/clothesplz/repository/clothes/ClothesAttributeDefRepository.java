package com.gitggal.clothesplz.repository.clothes;

import com.gitggal.clothesplz.entity.clothes.ClothesAttributeDef;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface ClothesAttributeDefRepository extends JpaRepository<ClothesAttributeDef, UUID>, ClothesAttributeDefRepositoryCustom {

}
