package com.gitggal.clothesplz.controller.clothes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.config.TestSecurityConfig;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDto;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeWithDefDto;
import com.gitggal.clothesplz.dto.clothes.ClothesCreateRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesDto;
import com.gitggal.clothesplz.dto.clothes.ClothesDtoCursorResponse;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ClothesErrorCode;
import com.gitggal.clothesplz.exception.GlobalExceptionHandler;
import com.gitggal.clothesplz.security.jwt.JwtAuthenticationFilter;
import com.gitggal.clothesplz.service.clothes.ClothesService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import({
    GlobalExceptionHandler.class,
    TestSecurityConfig.class
})
@WebMvcTest(
    controllers = ClothesController.class,
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class
        )
    }
)
@DisplayName("Clothes Controller 테스트")
class ClothesControllerTest {

  @MockitoBean
  private ClothesService clothesService;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  private UUID ownerId;
  private UUID definitionId;
  private ClothesDto clothesDto;

  @BeforeEach
  void setUp() {
    ownerId = UUID.randomUUID();
    definitionId = UUID.randomUUID();
    clothesDto = new ClothesDto(
        UUID.randomUUID(),
        ownerId,
        "흰 티셔츠",
        null,
        ClothesType.TOP,
        List.of(new ClothesAttributeWithDefDto(definitionId, "색상", List.of("WHITE", "BLACK"), "WHITE"))
    );
  }

  private MockMultipartFile toRequestPart(ClothesCreateRequest request) throws Exception {
    return new MockMultipartFile(
        "request",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        objectMapper.writeValueAsBytes(request)
    );
  }

  @Nested
  @DisplayName("의상 조회 관련 테스트")
  class GetClothesTests {

    @Test
    @DisplayName("성공 - 조건으로 조회 시 200과 커서 응답을 반환한다")
    void getClothes_returns200() throws Exception {
      ClothesDtoCursorResponse response = new ClothesDtoCursorResponse(
          List.of(clothesDto),
          "2024-01-01T00:00:00Z",
          UUID.randomUUID(),
          true,
          1L,
          "createdAt",
          "DESCENDING"
      );
      given(clothesService.getClothes(any())).willReturn(response);

      mockMvc.perform(get("/api/clothes")
              .queryParam("ownerId", ownerId.toString())
              .queryParam("limit", "20"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data[0].ownerId").value(ownerId.toString()))
          .andExpect(jsonPath("$.hasNext").value(true))
          .andExpect(jsonPath("$.sortBy").value("createdAt"))
          .andExpect(jsonPath("$.sortDirection").value("DESCENDING"));
    }

    @Test
    @DisplayName("실패 - 잘못된 cursor 형식이면 400을 반환한다")
    void getClothes_invalidCursor_returns400() throws Exception {
      given(clothesService.getClothes(any()))
          .willThrow(new BusinessException(ClothesErrorCode.INVALID_CURSOR_FORMAT));

      mockMvc.perform(get("/api/clothes")
              .queryParam("ownerId", ownerId.toString())
              .queryParam("limit", "20")
              .queryParam("cursor", "not-a-date")
              .queryParam("idAfter", UUID.randomUUID().toString()))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("의상 등록 관련 테스트")
  class CreateClothesTests {

    @Test
    @DisplayName("성공 - 이미지 없이 등록 시 201과 의상 정보를 반환한다")
    void createClothes_withoutImage_returns201() throws Exception {
      ClothesCreateRequest request = new ClothesCreateRequest(
          ownerId, "흰 티셔츠", ClothesType.TOP,
          List.of(new ClothesAttributeDto(definitionId, "WHITE"))
      );
      given(clothesService.createClothes(any(), isNull())).willReturn(clothesDto);

      mockMvc.perform(multipart("/api/clothes")
              .file(toRequestPart(request))
              .with(csrf()))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.ownerId").value(ownerId.toString()))
          .andExpect(jsonPath("$.name").value("흰 티셔츠"))
          .andExpect(jsonPath("$.type").value("TOP"))
          .andExpect(jsonPath("$.attributes[0].definitionId").value(definitionId.toString()))
          .andExpect(jsonPath("$.attributes[0].value").value("WHITE"));
    }

    @Test
    @DisplayName("성공 - 이미지 포함 등록 시 201과 의상 정보를 반환한다")
    void createClothes_withImage_returns201() throws Exception {
      ClothesCreateRequest request = new ClothesCreateRequest(
          ownerId, "흰 티셔츠", ClothesType.TOP,
          List.of(new ClothesAttributeDto(definitionId, "WHITE"))
      );
      MockMultipartFile image = new MockMultipartFile(
          "image", "shirt.jpg", MediaType.IMAGE_JPEG_VALUE, "img".getBytes()
      );
      ClothesDto clothesDtoWithImage = new ClothesDto(
          UUID.randomUUID(), ownerId, "흰 티셔츠",
          "https://cdn.example.com/shirt.jpg", ClothesType.TOP,
          List.of(new ClothesAttributeWithDefDto(definitionId, "색상", List.of("WHITE", "BLACK"), "WHITE"))
      );
      given(clothesService.createClothes(any(), any())).willReturn(clothesDtoWithImage);

      mockMvc.perform(multipart("/api/clothes")
              .file(toRequestPart(request))
              .file(image)
              .with(csrf()))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.name").value("흰 티셔츠"))
          .andExpect(jsonPath("$.imageUrl").value("https://cdn.example.com/shirt.jpg"));
      verify(clothesService).createClothes(any(), any());
    }

    @Test
    @DisplayName("실패 - ownerId가 null이면 400을 반환한다")
    void createClothes_ownerIdNull_returns400() throws Exception {
      ClothesCreateRequest request = new ClothesCreateRequest(
          null, "흰 티셔츠", ClothesType.TOP,
          List.of(new ClothesAttributeDto(definitionId, "WHITE"))
      );

      mockMvc.perform(multipart("/api/clothes")
              .file(toRequestPart(request))
              .with(csrf()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - name이 공백이면 400을 반환한다")
    void createClothes_nameBlank_returns400() throws Exception {
      ClothesCreateRequest request = new ClothesCreateRequest(
          ownerId, "   ", ClothesType.TOP,
          List.of(new ClothesAttributeDto(definitionId, "WHITE"))
      );

      mockMvc.perform(multipart("/api/clothes")
              .file(toRequestPart(request))
              .with(csrf()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - type이 null이면 400을 반환한다")
    void createClothes_typeNull_returns400() throws Exception {
      ClothesCreateRequest request = new ClothesCreateRequest(
          ownerId, "흰 티셔츠", null,
          List.of(new ClothesAttributeDto(definitionId, "WHITE"))
      );

      mockMvc.perform(multipart("/api/clothes")
              .file(toRequestPart(request))
              .with(csrf()))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패 - attributes가 null이면 400을 반환한다")
    void createClothes_attributesNull_returns400() throws Exception {
      ClothesCreateRequest request = new ClothesCreateRequest(
          ownerId, "흰 티셔츠", ClothesType.TOP, null
      );

      mockMvc.perform(multipart("/api/clothes")
              .file(toRequestPart(request))
              .with(csrf()))
          .andExpect(status().isBadRequest());
    }
  }
}
