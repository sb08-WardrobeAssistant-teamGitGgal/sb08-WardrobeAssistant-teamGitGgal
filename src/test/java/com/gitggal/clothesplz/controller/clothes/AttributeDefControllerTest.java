package com.gitggal.clothesplz.controller.clothes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.config.TestSecurityConfig;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefCreateRequest;
import com.gitggal.clothesplz.dto.clothes.ClothesAttributeDefDto;
import com.gitggal.clothesplz.exception.GlobalExceptionHandler;
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
        .andExpect(jsonPath("$.definitionId").value(definitionId.toString()))
        .andExpect(jsonPath("$.definitionName").value("색상"))
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
}
