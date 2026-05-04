package com.gitggal.clothesplz.repository.clothes;

import com.gitggal.clothesplz.entity.clothes.Clothes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface ClothesRepository extends JpaRepository<Clothes, UUID> {

}
