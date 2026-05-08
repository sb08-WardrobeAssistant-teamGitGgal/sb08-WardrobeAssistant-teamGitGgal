package com.gitggal.clothesplz.controller.clothes;

import com.gitggal.clothesplz.controller.clothes.api.ClothesControllerApi;
import com.gitggal.clothesplz.dto.clothes.ClothesCreateRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesDto;
import com.gitggal.clothesplz.dto.clothes.ClothesDtoCursorResponse;
import com.gitggal.clothesplz.dto.clothes.ClothesUpdateRequest;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/clothes")
public class ClothesController implements ClothesControllerApi {

  @Override
  @GetMapping
  public ResponseEntity<ClothesDtoCursorResponse> getClothes(
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam Integer limit,
      @RequestParam(required = false) ClothesType typeEqual,
      @RequestParam UUID ownerId
  ) {
    return null;
  }

  @Override
  @PostMapping
  public ResponseEntity<ClothesDto> createClothes(
      @RequestPart @Valid ClothesCreateRequest request,
      @RequestPart(required = false) MultipartFile image
  ) {
    return null;
  }

  @Override
  @DeleteMapping("/{clothesId}")
  public ResponseEntity<Void> deleteClothes(
      @PathVariable UUID clothesId
  ) {
    return null;
  }

  @Override
  @PatchMapping("/{clothesId}")
  public ResponseEntity<ClothesDto> updateClothes(
      @PathVariable UUID clothesId,
      @RequestPart @Valid ClothesUpdateRequest request,
      @RequestPart(required = false) MultipartFile image
  ) {
    return null;
  }

  @Override
  @GetMapping("/extractions")
  public ResponseEntity<ClothesDto> extractByUrl(
      @RequestParam String url
  ) {
    return null;
  }
}
