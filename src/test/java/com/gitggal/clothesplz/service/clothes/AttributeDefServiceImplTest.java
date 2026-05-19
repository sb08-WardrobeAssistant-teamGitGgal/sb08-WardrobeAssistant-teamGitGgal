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
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefUpdateRequest;
import com.gitggal.clothesplz.entity.clothes.ClothesAttributeDef;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ClothesErrorCode;
import com.gitggal.clothesplz.mapper.clothes.AttributeDefMapper;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeDefRepository;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeRepository;
import com.gitggal.clothesplz.service.clothes.impl.AttributeDefServiceImpl;
import java.util.List;
import java.util.Optional;
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

  @Mock
  private ClothesAttributeRepository clothesAttributeRepository;

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
  @DisplayName("성공 - 조건에 맞는 속성 목록을 조회하여 DTO 리스트로 반환한다")
  void getAttributeDefs_success_returnsMappedList() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    ClothesAttributeDef def1 = new ClothesAttributeDef("색상", List.of("WHITE", "BLACK"));
    ClothesAttributeDef def2 = new ClothesAttributeDef("소재", List.of("COTTON", "WOOL"));
    ReflectionTestUtils.setField(def1, "id", id1);
    ReflectionTestUtils.setField(def2, "id", id2);
    ClothesAttributeDefDto dto1 = new ClothesAttributeDefDto(id1, "색상", List.of("WHITE", "BLACK"), null);
    ClothesAttributeDefDto dto2 = new ClothesAttributeDefDto(id2, "소재", List.of("COTTON", "WOOL"), null);

    given(clothesAttributeDefRepository.findAllByConditions("name", "ASCENDING", null))
        .willReturn(List.of(def1, def2));
    given(attributeDefMapper.toClothesAttributeDefDtoForSearch(def1)).willReturn(dto1);
    given(attributeDefMapper.toClothesAttributeDefDtoForSearch(def2)).willReturn(dto2);

    List<ClothesAttributeDefDto> result = attributeDefService.getAttributeDefs("name", "ASCENDING", null);

    assertThat(result).hasSize(2);
    assertThat(result.get(0)).isEqualTo(dto1);
    assertThat(result.get(1)).isEqualTo(dto2);
    verify(clothesAttributeDefRepository).findAllByConditions("name", "ASCENDING", null);
    verify(attributeDefMapper).toClothesAttributeDefDtoForSearch(def1);
    verify(attributeDefMapper).toClothesAttributeDefDtoForSearch(def2);
  }

  @Test
  @DisplayName("성공 - 결과가 없으면 빈 리스트를 반환한다")
  void getAttributeDefs_noResults_returnsEmptyList() {
    given(clothesAttributeDefRepository.findAllByConditions("name", "DESCENDING", "없는키워드"))
        .willReturn(List.of());

    List<ClothesAttributeDefDto> result = attributeDefService.getAttributeDefs(
        "name", "DESCENDING", "없는키워드");

    assertThat(result).isEmpty();
    verifyNoInteractions(attributeDefMapper);
  }

  @Test
  @DisplayName("성공 - keywordLike 파라미터를 repository에 그대로 전달한다")
  void getAttributeDefs_withKeyword_passesKeywordToRepository() {
    given(clothesAttributeDefRepository.findAllByConditions("name", "ASCENDING", "색"))
        .willReturn(List.of());

    attributeDefService.getAttributeDefs("name", "ASCENDING", "색");

    verify(clothesAttributeDefRepository).findAllByConditions("name", "ASCENDING", "색");
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

  @Test
  @DisplayName("성공 - 존재하는 정의 ID면 관련 속성과 정의를 모두 삭제한다")
  void deleteAttributeDefs_success_deletesAttributesAndDef() {
    UUID definitionId = UUID.randomUUID();
    ClothesAttributeDef attributeDef = new ClothesAttributeDef("색상", List.of("WHITE", "BLACK"));
    ReflectionTestUtils.setField(attributeDef, "id", definitionId);
    given(clothesAttributeDefRepository.findById(definitionId)).willReturn(Optional.of(attributeDef));

    attributeDefService.deleteAttributeDefs(definitionId);

    verify(clothesAttributeRepository).deleteAllByDefinitionId(definitionId);
    verify(clothesAttributeDefRepository).delete(attributeDef);
  }

  @Test
  @DisplayName("실패 - 존재하지 않는 정의 ID면 CLOTHES_ATTRIBUTE_DEFINITION_NOT_FOUND 예외를 던진다")
  void deleteAttributeDefs_notFound_throwsException() {
    UUID definitionId = UUID.randomUUID();
    given(clothesAttributeDefRepository.findById(definitionId)).willReturn(Optional.empty());

    BusinessException exception = catchThrowableOfType(
        () -> attributeDefService.deleteAttributeDefs(definitionId),
        BusinessException.class
    );

    assertThat(exception).isNotNull();
    assertThat(exception.getErrorCode()).isEqualTo(ClothesErrorCode.CLOTHES_ATTRIBUTE_DEFINITION_NOT_FOUND);
    verify(clothesAttributeRepository, never()).deleteAllByDefinitionId(any());
    verify(clothesAttributeDefRepository, never()).delete(any(ClothesAttributeDef.class));
  }

  @Test
  @DisplayName("성공 - name과 selectableValues 모두 전달하면 두 필드 모두 수정하고 DTO를 반환한다")
  void updateAttributeDef_nameAndValues_updatesBothAndReturnsDto() {
    UUID definitionId = UUID.randomUUID();
    ClothesAttributeDef attributeDef = new ClothesAttributeDef("색상", List.of("WHITE", "BLACK"));
    ReflectionTestUtils.setField(attributeDef, "id", definitionId);
    ClothesAttributeDefUpdateRequest request = new ClothesAttributeDefUpdateRequest(
        "재질", List.of("COTTON", "WOOL")
    );
    ClothesAttributeDefDto response = new ClothesAttributeDefDto(
        definitionId, "재질", List.of("COTTON", "WOOL"), null
    );

    given(clothesAttributeDefRepository.findById(definitionId)).willReturn(Optional.of(attributeDef));
    given(attributeDefMapper.toClothesAttributeDefDto(attributeDef)).willReturn(response);

    ClothesAttributeDefDto result = attributeDefService.updateAttributeDef(definitionId, request);

    assertThat(attributeDef.getName()).isEqualTo("재질");
    assertThat(attributeDef.getSelectableValues()).containsExactly("COTTON", "WOOL");
    assertThat(result).isEqualTo(response);
    verify(clothesAttributeDefRepository).findById(definitionId);
    verify(attributeDefMapper).toClothesAttributeDefDto(attributeDef);
  }

  @Test
  @DisplayName("성공 - name만 전달하면 name만 수정하고 selectableValues는 유지한다")
  void updateAttributeDef_nameOnly_updatesNameOnly() {
    UUID definitionId = UUID.randomUUID();
    ClothesAttributeDef attributeDef = new ClothesAttributeDef("색상", List.of("WHITE", "BLACK"));
    ReflectionTestUtils.setField(attributeDef, "id", definitionId);
    ClothesAttributeDefUpdateRequest request = new ClothesAttributeDefUpdateRequest("재질", null);
    ClothesAttributeDefDto response = new ClothesAttributeDefDto(
        definitionId, "재질", List.of("WHITE", "BLACK"), null
    );

    given(clothesAttributeDefRepository.findById(definitionId)).willReturn(Optional.of(attributeDef));
    given(attributeDefMapper.toClothesAttributeDefDto(attributeDef)).willReturn(response);

    attributeDefService.updateAttributeDef(definitionId, request);

    assertThat(attributeDef.getName()).isEqualTo("재질");
    assertThat(attributeDef.getSelectableValues()).containsExactly("WHITE", "BLACK");
  }

  @Test
  @DisplayName("성공 - selectableValues만 전달하면 selectableValues만 수정하고 name은 유지한다")
  void updateAttributeDef_selectableValuesOnly_updatesValuesOnly() {
    UUID definitionId = UUID.randomUUID();
    ClothesAttributeDef attributeDef = new ClothesAttributeDef("색상", List.of("WHITE", "BLACK"));
    ReflectionTestUtils.setField(attributeDef, "id", definitionId);
    ClothesAttributeDefUpdateRequest request = new ClothesAttributeDefUpdateRequest(null, List.of("RED", "BLUE"));
    ClothesAttributeDefDto response = new ClothesAttributeDefDto(
        definitionId, "색상", List.of("RED", "BLUE"), null
    );

    given(clothesAttributeDefRepository.findById(definitionId)).willReturn(Optional.of(attributeDef));
    given(attributeDefMapper.toClothesAttributeDefDto(attributeDef)).willReturn(response);

    attributeDefService.updateAttributeDef(definitionId, request);

    assertThat(attributeDef.getName()).isEqualTo("색상");
    assertThat(attributeDef.getSelectableValues()).containsExactly("RED", "BLUE");
  }

  @Test
  @DisplayName("실패 - 존재하지 않는 정의 ID면 CLOTHES_ATTRIBUTE_DEFINITION_NOT_FOUND 예외를 던진다")
  void updateAttributeDef_notFound_throwsException() {
    UUID definitionId = UUID.randomUUID();
    ClothesAttributeDefUpdateRequest request = new ClothesAttributeDefUpdateRequest("재질", List.of("COTTON"));
    given(clothesAttributeDefRepository.findById(definitionId)).willReturn(Optional.empty());

    BusinessException exception = catchThrowableOfType(
        () -> attributeDefService.updateAttributeDef(definitionId, request),
        BusinessException.class
    );

    assertThat(exception).isNotNull();
    assertThat(exception.getErrorCode()).isEqualTo(ClothesErrorCode.CLOTHES_ATTRIBUTE_DEFINITION_NOT_FOUND);
    verifyNoInteractions(attributeDefMapper);
  }
}
