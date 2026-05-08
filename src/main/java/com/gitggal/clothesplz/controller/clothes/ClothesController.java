package com.gitggal.clothesplz.controller.clothes;

import com.gitggal.clothesplz.controller.clothes.api.ClothesControllerApi;
import com.gitggal.clothesplz.dto.clothes.ClothesCreateRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesDto;
import com.gitggal.clothesplz.dto.clothes.ClothesDtoCursorResponse;
import com.gitggal.clothesplz.dto.clothes.ClothesUpdateRequest;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import com.gitggal.clothesplz.service.clothes.ClothesService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/clothes")
@RequiredArgsConstructor
@Slf4j
public class ClothesController implements ClothesControllerApi {

  private final ClothesService clothesService;

  @Override
  @GetMapping
  public ResponseEntity<ClothesDtoCursorResponse> getClothes(
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam Integer limit,
      @RequestParam(required = false) ClothesType typeEqual,
      @RequestParam UUID ownerId
  ) {

    ClothesDtoCursorResponse response = clothesService.getClothes(
        cursor,
        idAfter,
        limit,
        typeEqual,
        ownerId
    );

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(response);
  }

  @Override
  @PostMapping
  public ResponseEntity<ClothesDto> createClothes(
      @RequestPart @Valid ClothesCreateRequest request,
      @RequestPart(required = false) MultipartFile image
  ) {

    ClothesDto response = clothesService.createClothes(request, image);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
  }

  @Override
  @DeleteMapping("/{clothesId}")
  public ResponseEntity<Void> deleteClothes(
      @PathVariable UUID clothesId
  ) {

    clothesService.deleteClothes(clothesId);

    return ResponseEntity
        .status(HttpStatus.OK)
        .build();
  }

  @Override
  @PatchMapping("/{clothesId}")
  public ResponseEntity<ClothesDto> updateClothes(
      @PathVariable UUID clothesId,
      @RequestPart @Valid ClothesUpdateRequest request,
      @RequestPart(required = false) MultipartFile image
  ) {

    ClothesDto response = clothesService.updateClothes(
        clothesId,
        request,
        image
    );

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(response);
  }

  @Override
  @GetMapping("/extractions")
  public ResponseEntity<ClothesDto> extractByUrl(
      @RequestParam String url
  ) {
    ClothesDto response = clothesService.extractByUrl(url);

    return ResponseEntity
        .status(HttpStatus.OK)
        .body(response);
  }
}
