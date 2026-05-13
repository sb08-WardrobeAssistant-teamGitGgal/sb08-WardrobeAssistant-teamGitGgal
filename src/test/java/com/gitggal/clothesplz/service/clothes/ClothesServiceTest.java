package com.gitggal.clothesplz.service.clothes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDto;
import com.gitggal.clothesplz.dto.clothes.ClothesCreateRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesDto;
import com.gitggal.clothesplz.dto.clothes.ClothesDtoCursorResponse;
import com.gitggal.clothesplz.dto.clothes.ClothesGetRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesUpdateRequest;
import com.gitggal.clothesplz.entity.clothes.Clothes;
import com.gitggal.clothesplz.entity.clothes.ClothesAttributeDef;
import com.gitggal.clothesplz.entity.clothes.ClothesAttribute;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ClothesErrorCode;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.service.ServiceTestSupport;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Clothes Service 테스트")
class ClothesServiceTest extends ServiceTestSupport {

  @Autowired
  private ClothesService clothesService;

  private UUID ownerId;
  private UUID definitionId;
  private User owner;
  private ClothesAttributeDef attributeDef;

  @BeforeEach
  void setUp() {
    ownerId = UUID.randomUUID();
    definitionId = UUID.randomUUID();
    owner = new User("홍길동", "hong@test.com", "password");
    attributeDef = new ClothesAttributeDef("색상", List.of("WHITE", "BLACK"));
    ReflectionTestUtils.setField(attributeDef, "id", definitionId);
  }

  private ClothesCreateRequest request(List<ClothesAttributeDto> attributes) {
    return new ClothesCreateRequest(ownerId, "흰 티셔츠", ClothesType.TOP, attributes);
  }

  @Test
  @DisplayName("의상 목록 조회에 성공한다")
  void getClothes_success() {
    ClothesGetRequest req = new ClothesGetRequest(null, null, 20, null, ownerId);
    Clothes clothes = new Clothes(owner, "흰 티셔츠", ClothesType.TOP, null, null);

    given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
    given(clothesRepository.findAllByCursor(req, null)).willReturn(List.of(clothes));
    given(clothesRepository.countByCursor(req)).willReturn(1L);
    given(clothesAttributeRepository.findAllByClothesIdIn(any())).willReturn(List.of());

    ClothesDtoCursorResponse result = clothesService.getClothes(req);

    assertThat(result.data()).hasSize(1);
    assertThat(result.totalCount()).isEqualTo(1L);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.sortBy()).isEqualTo("createdAt");
    assertThat(result.sortDirection()).isEqualTo("DESCENDING");
  }

  @Test
  @DisplayName("cursor 형식이 잘못되면 INVALID_CURSOR_FORMAT 예외가 발생한다")
  void getClothes_invalidCursor_throwsException() {
    ClothesGetRequest req = new ClothesGetRequest("invalid-cursor", UUID.randomUUID(), 20, null, ownerId);
    given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));

    Throwable thrown = catchThrowable(() -> clothesService.getClothes(req));

    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode())
        .isEqualTo(ClothesErrorCode.INVALID_CURSOR_FORMAT);
  }

  @Test
  @DisplayName("이미지 없이 의상 등록에 성공한다")
  void createClothes_withoutImage_success() {
    ClothesCreateRequest req = request(List.of(new ClothesAttributeDto(definitionId, "WHITE")));
    given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
    given(clothesAttributeDefRepository.findAllById(List.of(definitionId))).willReturn(List.of(attributeDef));

    ClothesDto result = clothesService.createClothes(req, null);

    assertThat(result.name()).isEqualTo("흰 티셔츠");
    assertThat(result.type()).isEqualTo(ClothesType.TOP);
    assertThat(result.imageUrl()).isNull();
    verify(imageUploader, never()).upload(any());
  }

  @Test
  @DisplayName("이미지와 함께 의상 등록에 성공한다")
  void createClothes_withImage_success() {
    ClothesCreateRequest req = request(List.of(new ClothesAttributeDto(definitionId, "WHITE")));
    MockMultipartFile image = new MockMultipartFile("image", "shirt.jpg", "image/jpeg", "img".getBytes());
    String imageUrl = "http://s3.example.com/shirt.jpg";

    given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
    given(imageUploader.upload(image)).willReturn(imageUrl);
    given(clothesAttributeDefRepository.findAllById(List.of(definitionId))).willReturn(List.of(attributeDef));

    ClothesDto result = clothesService.createClothes(req, image);

    assertThat(result.imageUrl()).isEqualTo(imageUrl);
  }

  @Test
  @DisplayName("존재하지 않는 사용자로 등록하면 USER_NOT_FOUND 예외가 발생한다")
  void createClothes_userNotFound_throwsException() {
    ClothesCreateRequest req = request(List.of(new ClothesAttributeDto(definitionId, "WHITE")));
    given(userRepository.findById(ownerId)).willReturn(Optional.empty());

    Throwable thrown = catchThrowable(() -> clothesService.createClothes(req, null));

    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
  }

  @Test
  @DisplayName("중복된 속성 정의 ID가 포함되면 DUPLICATE_CLOTHES_ATTRIBUTE_DEFINITION_ID 예외가 발생한다")
  void createClothes_duplicateAttributeId_throwsException() {
    ClothesCreateRequest req = request(List.of(
        new ClothesAttributeDto(definitionId, "WHITE"),
        new ClothesAttributeDto(definitionId, "BLACK")
    ));
    given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));

    Throwable thrown = catchThrowable(() -> clothesService.createClothes(req, null));

    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode())
        .isEqualTo(ClothesErrorCode.DUPLICATE_CLOTHES_ATTRIBUTE_DEFINITION_ID);
  }

  @Test
  @DisplayName("존재하지 않는 속성 정의 ID가 포함되면 CLOTHES_ATTRIBUTE_DEFINITION_NOT_FOUND 예외가 발생한다")
  void createClothes_attributeDefNotFound_throwsException() {
    UUID unknownId = UUID.randomUUID();
    ClothesCreateRequest req = request(List.of(new ClothesAttributeDto(unknownId, "WHITE")));
    given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
    given(clothesAttributeDefRepository.findAllById(List.of(unknownId))).willReturn(List.of());

    Throwable thrown = catchThrowable(() -> clothesService.createClothes(req, null));

    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode())
        .isEqualTo(ClothesErrorCode.CLOTHES_ATTRIBUTE_DEFINITION_NOT_FOUND);
  }

  @Test
  @DisplayName("허용되지 않는 속성 값이면 INVALID_CLOTHES_ATTRIBUTE_VALUE 예외가 발생한다")
  void createClothes_invalidAttributeValue_throwsException() {
    ClothesCreateRequest req = request(List.of(new ClothesAttributeDto(definitionId, "YELLOW")));
    given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
    given(clothesAttributeDefRepository.findAllById(List.of(definitionId))).willReturn(List.of(attributeDef));

    Throwable thrown = catchThrowable(() -> clothesService.createClothes(req, null));

    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode())
        .isEqualTo(ClothesErrorCode.INVALID_CLOTHES_ATTRIBUTE_VALUE);
  }

  @Test
  @DisplayName("이미지 업로드 후 예외 발생 시 업로드된 이미지를 삭제한다")
  void createClothes_exceptionAfterImageUpload_deletesUploadedImage() {
    ClothesCreateRequest req = request(List.of(
        new ClothesAttributeDto(definitionId, "WHITE"),
        new ClothesAttributeDto(definitionId, "BLACK")
    ));
    MockMultipartFile image = new MockMultipartFile("image", "shirt.jpg", "image/jpeg", "img".getBytes());
    String imageUrl = "http://s3.example.com/shirt.jpg";

    given(userRepository.findById(ownerId)).willReturn(Optional.of(owner));
    given(imageUploader.upload(image)).willReturn(imageUrl);

    catchThrowable(() -> clothesService.createClothes(req, image));

    verify(imageUploader).delete(imageUrl);
  }

  @Test
  @DisplayName("의상 삭제에 성공하면 속성과 의상을 삭제한다")
  void deleteClothes_success_deletesAttributesAndClothes() {
    UUID clothesId = UUID.randomUUID();
    Clothes clothes = new Clothes(owner, "흰 티셔츠", ClothesType.TOP, null, null);
    given(clothesRepository.findById(clothesId)).willReturn(Optional.of(clothes));

    clothesService.deleteClothes(clothesId);

    verify(clothesAttributeRepository).deleteAllByClothesId(clothesId);
    verify(clothesRepository).delete(clothes);
    verify(imageUploader, never()).delete(any());
  }

  @Test
  @DisplayName("이미지가 있는 의상 삭제 시 이미지도 삭제한다")
  void deleteClothes_withImage_deletesImage() {
    UUID clothesId = UUID.randomUUID();
    Clothes clothes = new Clothes(owner, "흰 티셔츠", ClothesType.TOP, "http://s3.example.com/shirt.jpg", null);
    given(clothesRepository.findById(clothesId)).willReturn(Optional.of(clothes));

    clothesService.deleteClothes(clothesId);

    verify(imageUploader).delete("http://s3.example.com/shirt.jpg");
    verify(clothesAttributeRepository).deleteAllByClothesId(clothesId);
    verify(clothesRepository).delete(clothes);
  }

  @Test
  @DisplayName("이미지 URL이 공백이면 이미지를 삭제하지 않는다")
  void deleteClothes_withBlankImageUrl_doesNotDeleteImage() {
    UUID clothesId = UUID.randomUUID();
    Clothes clothes = new Clothes(owner, "흰 티셔츠", ClothesType.TOP, "   ", null);
    given(clothesRepository.findById(clothesId)).willReturn(Optional.of(clothes));

    clothesService.deleteClothes(clothesId);

    verify(imageUploader, never()).delete(any());
    verify(clothesAttributeRepository).deleteAllByClothesId(clothesId);
    verify(clothesRepository).delete(clothes);
  }

  @Test
  @DisplayName("삭제 대상 의상이 없으면 CLOTHES_NOT_FOUND 예외가 발생한다")
  void deleteClothes_notFound_throwsException() {
    UUID clothesId = UUID.randomUUID();
    given(clothesRepository.findById(clothesId)).willReturn(Optional.empty());

    Throwable thrown = catchThrowable(() -> clothesService.deleteClothes(clothesId));

    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode()).isEqualTo(ClothesErrorCode.CLOTHES_NOT_FOUND);
    verify(clothesAttributeRepository, never()).deleteAllByClothesId(any());
    verify(clothesRepository, never()).delete(any());
  }

  @Test
  @DisplayName("의상 수정에 성공하면 요청한 값으로 수정한다")
  void updateClothes_success() {
    UUID clothesId = UUID.randomUUID();
    Clothes clothes = new Clothes(owner, "흰 티셔츠", ClothesType.TOP, null, null);
    ClothesUpdateRequest req = new ClothesUpdateRequest(
        "검은 티셔츠",
        ClothesType.OUTER,
        List.of(new ClothesAttributeDto(definitionId, "WHITE"))
    );
    given(clothesRepository.findById(clothesId)).willReturn(Optional.of(clothes));
    given(clothesAttributeDefRepository.findAllById(List.of(definitionId))).willReturn(List.of(attributeDef));

    ClothesDto result = clothesService.updateClothes(clothesId, req, null);

    assertThat(result.name()).isEqualTo("검은 티셔츠");
    assertThat(result.type()).isEqualTo(ClothesType.OUTER);
    assertThat(result.attributes()).hasSize(1);
    verify(clothesAttributeRepository).deleteAllByClothesId(clothesId);
  }

  @Test
  @DisplayName("수정 대상 의상이 없으면 CLOTHES_NOT_FOUND 예외가 발생한다")
  void updateClothes_notFound_throwsException() {
    UUID clothesId = UUID.randomUUID();
    ClothesUpdateRequest req = new ClothesUpdateRequest("검은 티셔츠", ClothesType.OUTER, null);
    given(clothesRepository.findById(clothesId)).willReturn(Optional.empty());

    Throwable thrown = catchThrowable(() -> clothesService.updateClothes(clothesId, req, null));

    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode()).isEqualTo(ClothesErrorCode.CLOTHES_NOT_FOUND);
  }

  @Test
  @DisplayName("속성 요청이 null이면 기존 속성을 조회해 반환한다")
  void updateClothes_withNullAttributes_returnsExistingAttributes() {
    UUID clothesId = UUID.randomUUID();
    Clothes clothes = new Clothes(owner, "흰 티셔츠", ClothesType.TOP, null, null);
    ClothesAttributeDef existingDef = new ClothesAttributeDef("색상", List.of("WHITE", "BLACK"));
    ReflectionTestUtils.setField(existingDef, "id", UUID.randomUUID());
    ClothesAttribute existingAttr = new ClothesAttribute(clothes, existingDef, "BLACK");

    ClothesUpdateRequest req = new ClothesUpdateRequest("검은 티셔츠", ClothesType.OUTER, null);
    given(clothesRepository.findById(clothesId)).willReturn(Optional.of(clothes));
    given(clothesAttributeRepository.findAllByClothesIdIn(List.of(clothesId))).willReturn(List.of(existingAttr));

    ClothesDto result = clothesService.updateClothes(clothesId, req, null);

    assertThat(result.attributes()).hasSize(1);
    assertThat(result.attributes().get(0).value()).isEqualTo("BLACK");
    verify(clothesAttributeRepository, never()).deleteAllByClothesId(any());
    verify(clothesAttributeRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("새 이미지를 업로드하면 기존 이미지를 삭제한다")
  void updateClothes_withNewImage_deletesOldImage() {
    UUID clothesId = UUID.randomUUID();
    String oldImageUrl = "http://s3.example.com/old.jpg";
    String newImageUrl = "http://s3.example.com/new.jpg";
    Clothes clothes = new Clothes(owner, "흰 티셔츠", ClothesType.TOP, oldImageUrl, null);
    MockMultipartFile image = new MockMultipartFile("image", "shirt.jpg", "image/jpeg", "img".getBytes());
    ClothesUpdateRequest req = new ClothesUpdateRequest("검은 티셔츠", ClothesType.OUTER,
        List.of(new ClothesAttributeDto(definitionId, "WHITE")));

    given(clothesRepository.findById(clothesId)).willReturn(Optional.of(clothes));
    given(imageUploader.upload(image)).willReturn(newImageUrl);
    given(clothesAttributeDefRepository.findAllById(List.of(definitionId))).willReturn(List.of(attributeDef));

    ClothesDto result = clothesService.updateClothes(clothesId, req, image);

    assertThat(result.imageUrl()).isEqualTo(newImageUrl);
    verify(imageUploader).delete(oldImageUrl);
  }

  @Test
  @DisplayName("수정 요청 속성에 중복된 정의 ID가 있으면 예외가 발생한다")
  void updateClothes_duplicateAttributeId_throwsException() {
    UUID clothesId = UUID.randomUUID();
    Clothes clothes = new Clothes(owner, "흰 티셔츠", ClothesType.TOP, null, null);
    ClothesUpdateRequest req = new ClothesUpdateRequest(
        "검은 티셔츠",
        ClothesType.OUTER,
        List.of(
            new ClothesAttributeDto(definitionId, "WHITE"),
            new ClothesAttributeDto(definitionId, "BLACK")
        )
    );
    given(clothesRepository.findById(clothesId)).willReturn(Optional.of(clothes));

    Throwable thrown = catchThrowable(() -> clothesService.updateClothes(clothesId, req, null));

    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode())
        .isEqualTo(ClothesErrorCode.DUPLICATE_CLOTHES_ATTRIBUTE_DEFINITION_ID);
  }

  @Test
  @DisplayName("수정 요청 속성 정의 ID가 없으면 예외가 발생한다")
  void updateClothes_attributeDefNotFound_throwsException() {
    UUID clothesId = UUID.randomUUID();
    UUID unknownId = UUID.randomUUID();
    Clothes clothes = new Clothes(owner, "흰 티셔츠", ClothesType.TOP, null, null);
    ClothesUpdateRequest req = new ClothesUpdateRequest(
        "검은 티셔츠",
        ClothesType.OUTER,
        List.of(new ClothesAttributeDto(unknownId, "WHITE"))
    );

    given(clothesRepository.findById(clothesId)).willReturn(Optional.of(clothes));
    given(clothesAttributeDefRepository.findAllById(List.of(unknownId))).willReturn(List.of());

    Throwable thrown = catchThrowable(() -> clothesService.updateClothes(clothesId, req, null));

    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode())
        .isEqualTo(ClothesErrorCode.CLOTHES_ATTRIBUTE_DEFINITION_NOT_FOUND);
  }

  @Test
  @DisplayName("수정 요청 속성 값이 허용되지 않으면 예외가 발생한다")
  void updateClothes_invalidAttributeValue_throwsException() {
    UUID clothesId = UUID.randomUUID();
    Clothes clothes = new Clothes(owner, "흰 티셔츠", ClothesType.TOP, null, null);
    ClothesUpdateRequest req = new ClothesUpdateRequest(
        "검은 티셔츠",
        ClothesType.OUTER,
        List.of(new ClothesAttributeDto(definitionId, "YELLOW"))
    );

    given(clothesRepository.findById(clothesId)).willReturn(Optional.of(clothes));
    given(clothesAttributeDefRepository.findAllById(List.of(definitionId))).willReturn(List.of(attributeDef));

    Throwable thrown = catchThrowable(() -> clothesService.updateClothes(clothesId, req, null));

    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode())
        .isEqualTo(ClothesErrorCode.INVALID_CLOTHES_ATTRIBUTE_VALUE);
  }

  @Test
  @DisplayName("이미지 업로드 후 수정 예외가 발생하면 업로드된 이미지를 삭제한다")
  void updateClothes_exceptionAfterUpload_deletesUploadedImage() {
    UUID clothesId = UUID.randomUUID();
    String uploadedImageUrl = "http://s3.example.com/new.jpg";
    Clothes clothes = new Clothes(owner, "흰 티셔츠", ClothesType.TOP, null, null);
    MockMultipartFile image = new MockMultipartFile("image", "shirt.jpg", "image/jpeg", "img".getBytes());
    ClothesUpdateRequest req = new ClothesUpdateRequest(
        "검은 티셔츠",
        ClothesType.OUTER,
        List.of(
            new ClothesAttributeDto(definitionId, "WHITE"),
            new ClothesAttributeDto(definitionId, "BLACK")
        )
    );

    given(clothesRepository.findById(clothesId)).willReturn(Optional.of(clothes));
    given(imageUploader.upload(image)).willReturn(uploadedImageUrl);

    Throwable thrown = catchThrowable(() -> clothesService.updateClothes(clothesId, req, image));

    assertThat(thrown).isNotNull().isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode())
        .isEqualTo(ClothesErrorCode.DUPLICATE_CLOTHES_ATTRIBUTE_DEFINITION_ID);
    verify(imageUploader, times(1)).delete(uploadedImageUrl);
  }
}
