package com.gitggal.clothesplz.controller.clothes;

import com.gitggal.clothesplz.controller.clothes.api.AttributeDefControllerApi;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefCreateRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefDto;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefUpdateRequest;
import com.gitggal.clothesplz.security.ClothesUserDetails;
import com.gitggal.clothesplz.service.clothes.AttributeDefService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
      @Valid @RequestBody ClothesAttributeDefCreateRequest request,
      @AuthenticationPrincipal ClothesUserDetails userDetails
  ) {
    log.info("[Controller] 의상 속성 생성 요청 시작");

    ClothesAttributeDefDto response = attributeDefService.createAttributeDef(
        request,
        userDetails.getUserDto().id()
    );

    log.info("[Controller] 의상 속성 생성 요청 완료");
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(response);
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  @Override
  public ResponseEntity<List<ClothesAttributeDefDto>> getAttributeDefs(
      @RequestParam("sortBy") String sortBy,
      @RequestParam("sortDirection") String sortDirection,
      @RequestParam(value = "keywordLike", required = false) String keywordLike
  ) {
    log.info("[Controller] 의상 속성 조회 요청 시작");

    List<ClothesAttributeDefDto> response = attributeDefService.getAttributeDefs(
        sortBy,
        sortDirection,
        keywordLike
    );

    log.info("[Controller] 의상 속성 조회 요청 완료");
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(response);
  }


  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{definitionId}")
  @Override
  public ResponseEntity<Void> deleteAttributeDefs(
      @PathVariable UUID definitionId,
      @AuthenticationPrincipal ClothesUserDetails userDetails
  ) {
    log.info("[Controller] 의상 속성 조회 삭제 시작");

    attributeDefService.deleteAttributeDefs(definitionId, userDetails.getUserDto().id());

    log.info("[Controller] 의상 속성 조회 삭제 완료");
    return ResponseEntity
        .noContent()
        .build();
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  @PatchMapping("/{definitionId}")
  public ResponseEntity<ClothesAttributeDefDto> updateAttributeDefs(
      @PathVariable UUID definitionId,
      @Valid @RequestBody ClothesAttributeDefUpdateRequest request,
      @AuthenticationPrincipal ClothesUserDetails userDetails
  ) {
    log.info("[Controller] 의상 속성 수정 시작");

    ClothesAttributeDefDto response = attributeDefService.updateAttributeDef(
        definitionId,
        request,
        userDetails.getUserDto().id()
    );

    log.info("[Controller] 의상 속성 수정 완료");
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(response);
  }
}
