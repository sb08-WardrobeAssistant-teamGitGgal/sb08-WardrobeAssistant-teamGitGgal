package com.gitggal.clothesplz.repository.clothes;

import com.gitggal.clothesplz.dto.clothes.ClothesGetRequest;
import com.gitggal.clothesplz.entity.clothes.Clothes;
import java.time.Instant;
import java.util.List;

public interface ClothesRepositoryCustom {

  List<Clothes> findAllByCursor(ClothesGetRequest request, Instant instantCursor);

  Long countByCursor(ClothesGetRequest request);
}
