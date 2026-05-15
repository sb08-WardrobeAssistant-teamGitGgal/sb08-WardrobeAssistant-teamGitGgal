package com.gitggal.clothesplz.service.clothes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefCreateRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefDto;
import com.gitggal.clothesplz.entity.clothes.ClothesAttributeDef;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ClothesErrorCode;
import com.gitggal.clothesplz.mapper.clothes.AttributeDefMapper;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeDefRepository;
import com.gitggal.clothesplz.service.clothes.impl.AttributeDefServiceImpl;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttributeDef Service 테스트")
class AttributeDefServiceImplTest {

  @Mock
  private ClothesAttributeDefRepository clothesAttributeDefRepository;

  @Mock
  private AttributeDefMapper attributeDefMapper;

  @InjectMocks
  private AttributeDefServiceImpl attributeDefService;

  @Test
  @DisplayName("성공 - 중복되지 않은 이름이면 속성을 저장하고 DTO를 반환한다")
  void createAttributeDef_success_returnsDto() {
    ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(
        "색상",
        List.of("WHITE", "BLACK")
    );
    UUID definitionId = UUID.randomUUID();
    ClothesAttributeDef saved = new ClothesAttributeDef("색상", List.of("WHITE", "BLACK"));
    ReflectionTestUtils.setField(saved, "id", definitionId);
    ClothesAttributeDefDto response = new ClothesAttributeDefDto(
        definitionId,
        "색상",
        List.of("WHITE", "BLACK"),
        null
    );

    given(clothesAttributeDefRepository.existsByName("색상")).willReturn(false);
    given(clothesAttributeDefRepository.save(any(ClothesAttributeDef.class))).willReturn(saved);
    given(attributeDefMapper.toClothesAttributeDefDto(saved)).willReturn(response);

    ClothesAttributeDefDto result = attributeDefService.createAttributeDef(request);

    ArgumentCaptor<ClothesAttributeDef> captor = ArgumentCaptor.forClass(ClothesAttributeDef.class);
    verify(clothesAttributeDefRepository).save(captor.capture());
    assertThat(captor.getValue().getName()).isEqualTo("색상");
    assertThat(captor.getValue().getSelectableValues()).containsExactly("WHITE", "BLACK");
    assertThat(result).isEqualTo(response);
    verify(attributeDefMapper).toClothesAttributeDefDto(saved);
  }

  @Test
  @DisplayName("실패 - 중복된 이름이면 DUPLICATE_ATTRIBUTE_NAME 예외를 던진다")
  void createAttributeDef_duplicateName_throwsException() {
    ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(
        "색상",
        List.of("WHITE", "BLACK")
    );
    given(clothesAttributeDefRepository.existsByName("색상")).willReturn(true);

    BusinessException exception = catchThrowableOfType(
        () -> attributeDefService.createAttributeDef(request),
        BusinessException.class
    );

    assertThat(exception).isNotNull();
    assertThat(exception.getErrorCode()).isEqualTo(ClothesErrorCode.DUPLICATE_ATTRIBUTE_NAME);
    verify(clothesAttributeDefRepository, never()).save(any(ClothesAttributeDef.class));
    verifyNoInteractions(attributeDefMapper);
  }

  @Test
  @DisplayName("실패 - 저장 시 유니크 제약 위반이면 DUPLICATE_ATTRIBUTE_NAME 예외를 던진다")
  void createAttributeDef_duplicateNameOnSave_throwsException() {
    ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(
        "색상",
        List.of("WHITE", "BLACK")
    );
    given(clothesAttributeDefRepository.existsByName("색상")).willReturn(false);
    given(clothesAttributeDefRepository.save(any(ClothesAttributeDef.class)))
        .willThrow(new DataIntegrityViolationException("duplicate key"));

    BusinessException exception = catchThrowableOfType(
        () -> attributeDefService.createAttributeDef(request),
        BusinessException.class
    );

    assertThat(exception).isNotNull();
    assertThat(exception.getErrorCode()).isEqualTo(ClothesErrorCode.DUPLICATE_ATTRIBUTE_NAME);
    verifyNoInteractions(attributeDefMapper);
  }
}
