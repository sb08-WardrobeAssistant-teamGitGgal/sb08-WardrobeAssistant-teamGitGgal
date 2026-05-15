package com.gitggal.clothesplz.controller.clothes;

import com.gitggal.clothesplz.controller.clothes.api.AttributeDefControllerApi;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefCreateRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefDto;
import com.gitggal.clothesplz.service.clothes.AttributeDefService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clothes/attribute-defs")
@RequiredArgsConstructor
@Slf4j
public class AttributeDefController implements AttributeDefControllerApi {

  private final AttributeDefService attributeDefService;

  @Override
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ClothesAttributeDefDto> createAttributeDef(
      @Valid @RequestBody ClothesAttributeDefCreateRequest request
  ) {
    log.info("[Controller] 의상 속성 생성 요청 시작");

    ClothesAttributeDefDto response = attributeDefService.createAttributeDef(request);

    log.info("[Controller] 의상 속성 생성 요청 완료");
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
  }
}
