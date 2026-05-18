package com.gitggal.clothesplz.controller.clothes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.config.TestSecurityConfig;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefCreateRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefDto;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.GlobalExceptionHandler;
import com.gitggal.clothesplz.exception.code.ClothesErrorCode;
import com.gitggal.clothesplz.security.jwt.JwtAuthenticationFilter;
import com.gitggal.clothesplz.service.clothes.AttributeDefService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("AttributeDef Controller 테스트")
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@WebMvcTest(
    controllers = AttributeDefController.class,
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = JwtAuthenticationFilter.class
        )
    }
)
class AttributeDefControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private AttributeDefService attributeDefService;

  @Test
  @DisplayName("성공 - ADMIN 권한으로 의상 속성 생성 시 201과 생성 결과를 반환한다")
  void createAttributeDef_asAdmin_returns201() throws Exception {
    UUID definitionId = UUID.randomUUID();
    ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(
        "색상",
        List.of("WHITE", "BLACK")
    );
    ClothesAttributeDefDto response = new ClothesAttributeDefDto(
        definitionId,
        "색상",
        List.of("WHITE", "BLACK"),
        null
    );
    given(attributeDefService.createAttributeDef(any(ClothesAttributeDefCreateRequest.class)))
        .willReturn(response);

    mockMvc.perform(post("/api/clothes/attribute-defs")
            .with(user("admin")
                .roles("ADMIN"))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(definitionId.toString()))
        .andExpect(jsonPath("$.name").value("색상"))
        .andExpect(jsonPath("$.selectableValues[0]").value("WHITE"))
        .andExpect(jsonPath("$.selectableValues[1]").value("BLACK"));

    verify(attributeDefService).createAttributeDef(any(ClothesAttributeDefCreateRequest.class));
  }

  @Test
  @DisplayName("실패 - selectableValues에 공백 값이 포함되면 400을 반환한다")
  void createAttributeDef_blankSelectableValue_returns400() throws Exception {
    ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(
        "색상",
        List.of("WHITE", " ")
    );

    mockMvc.perform(post("/api/clothes/attribute-defs")
            .with(user("admin").roles("ADMIN"))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(attributeDefService);
  }

  @Test
  @DisplayName("실패 - name이 null이면 400을 반환한다")
  void createAttributeDef_nullName_returns400() throws Exception {
    ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(
        null,
        List.of("WHITE", "BLACK")
    );

    mockMvc.perform(post("/api/clothes/attribute-defs")
            .with(user("admin").roles("ADMIN"))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(attributeDefService);
  }

  @Test
  @DisplayName("실패 - name이 공백이면 400을 반환한다")
  void createAttributeDef_blankName_returns400() throws Exception {
    ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(
        " ",
        List.of("WHITE", "BLACK")
    );

    mockMvc.perform(post("/api/clothes/attribute-defs")
            .with(user("admin").roles("ADMIN"))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(attributeDefService);
  }

  @Test
  @DisplayName("실패 - selectableValues가 null이면 400을 반환한다")
  void createAttributeDef_nullSelectableValues_returns400() throws Exception {
    ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(
        "색상",
        null
    );

    mockMvc.perform(post("/api/clothes/attribute-defs")
            .with(user("admin").roles("ADMIN"))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(attributeDefService);
  }

  @Test
  @DisplayName("실패 - selectableValues가 빈 리스트면 400을 반환한다")
  void createAttributeDef_emptySelectableValues_returns400() throws Exception {
    ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(
        "색상",
        List.of()
    );

    mockMvc.perform(post("/api/clothes/attribute-defs")
            .with(user("admin").roles("ADMIN"))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(attributeDefService);
  }

  @Test
  @DisplayName("성공 - 인증된 사용자가 의상 속성 목록 조회 시 200과 목록을 반환한다")
  void getAttributeDefs_asAuthenticatedUser_returns200() throws Exception {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    List<ClothesAttributeDefDto> response = List.of(
        new ClothesAttributeDefDto(id1, "색상", List.of("WHITE", "BLACK"), null),
        new ClothesAttributeDefDto(id2, "소재", List.of("COTTON", "WOOL"), null)
    );
    given(attributeDefService.getAttributeDefs(eq("name"), eq("ASCENDING"), isNull()))
        .willReturn(response);

    mockMvc.perform(get("/api/clothes/attribute-defs")
            .with(user("user").roles("USER"))
            .with(csrf())
            .param("sortBy", "name")
            .param("sortDirection", "ASCENDING"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(id1.toString()))
        .andExpect(jsonPath("$[0].name").value("색상"))
        .andExpect(jsonPath("$[1].id").value(id2.toString()))
        .andExpect(jsonPath("$[1].name").value("소재"));

    verify(attributeDefService).getAttributeDefs("name", "ASCENDING", null);
  }

  @Test
  @DisplayName("성공 - keywordLike 파라미터를 포함하면 서비스에 전달한다")
  void getAttributeDefs_withKeyword_passesKeyword() throws Exception {
    given(attributeDefService.getAttributeDefs(eq("name"), eq("DESCENDING"), eq("색")))
        .willReturn(List.of());

    mockMvc.perform(get("/api/clothes/attribute-defs")
            .with(user("user").roles("USER"))
            .with(csrf())
            .param("sortBy", "name")
            .param("sortDirection", "DESCENDING")
            .param("keywordLike", "색"))
        .andExpect(status().isOk());

    verify(attributeDefService).getAttributeDefs("name", "DESCENDING", "색");
  }

  @Test
  @DisplayName("실패 - 필수 파라미터 sortBy가 없으면 400을 반환한다")
  void getAttributeDefs_missingSortBy_returns400() throws Exception {
    mockMvc.perform(get("/api/clothes/attribute-defs")
            .with(user("user").roles("USER"))
            .with(csrf())
            .param("sortDirection", "ASCENDING"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(attributeDefService);
  }

  @Test
  @DisplayName("실패 - 필수 파라미터 sortDirection이 없으면 400을 반환한다")
  void getAttributeDefs_missingSortDirection_returns400() throws Exception {
    mockMvc.perform(get("/api/clothes/attribute-defs")
            .with(user("user").roles("USER"))
            .with(csrf())
            .param("sortBy", "name"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(attributeDefService);
  }

  @Test
  @DisplayName("실패 - 중복된 name이면 400을 반환한다")
  void createAttributeDef_duplicateName_returns400() throws Exception {
    ClothesAttributeDefCreateRequest request = new ClothesAttributeDefCreateRequest(
        "색상",
        List.of("WHITE", "BLACK")
    );
    given(attributeDefService.createAttributeDef(any(ClothesAttributeDefCreateRequest.class)))
        .willThrow(new BusinessException(ClothesErrorCode.DUPLICATE_ATTRIBUTE_NAME));

    mockMvc.perform(post("/api/clothes/attribute-defs")
            .with(user("admin").roles("ADMIN"))
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }
}
