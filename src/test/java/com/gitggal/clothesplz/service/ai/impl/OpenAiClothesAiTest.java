package com.gitggal.clothesplz.service.ai.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.dto.clothes.ClothesDto;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeRepository;
import java.io.IOException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAiClothesAi 테스트")
class OpenAiClothesAiTest {

  @Mock
  ChatClient.Builder chatClientBuilder;
  @Mock
  ChatClient chatClient;
  @Mock
  ChatClient.ChatClientRequestSpec requestSpec;
  @Mock
  ChatClient.CallResponseSpec callResponseSpec;
  @Mock
  ClothesAttributeRepository clothesAttributeRepository;

  OpenAiClothesAi sut;
  ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    given(chatClientBuilder.build()).willReturn(chatClient);
    sut = new OpenAiClothesAi(chatClientBuilder, objectMapper, clothesAttributeRepository);
  }

  @Test
  @DisplayName("og:title과 og:image가 있으면 이름과 타입을 정상 추출한다")
  void extractClothesByUrl_success() throws Exception {
    String url = "https://example.com/product";
    String html = "<html><head>"
        + "<meta property=\"og:title\" content=\"테스트 재킷\"/>"
        + "<meta property=\"og:image\" content=\"https://example.com/img.jpg\"/>"
        + "</head></html>";
    Document doc = Jsoup.parse(html, url);

    given(chatClient.prompt()).willReturn(requestSpec);
    given(requestSpec.system(anyString())).willReturn(requestSpec);
    given(requestSpec.user(anyString())).willReturn(requestSpec);
    given(requestSpec.call()).willReturn(callResponseSpec);
    given(callResponseSpec.content()).willReturn("{\"type\":\"OUTER\"}");

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
      Connection mockConn = mock(Connection.class);
      jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(mockConn);
      given(mockConn.userAgent(anyString())).willReturn(mockConn);
      given(mockConn.timeout(anyInt())).willReturn(mockConn);
      given(mockConn.get()).willReturn(doc);

      ClothesDto result = sut.extractClothesByUrl(url);

      assertThat(result.name()).isEqualTo("테스트 재킷");
      assertThat(result.imageUrl()).isEqualTo("https://example.com/img.jpg");
      assertThat(result.type()).isEqualTo(ClothesType.OUTER);
    }
  }

  @Test
  @DisplayName("Jsoup 연결 실패 시 알 수 없는 의상과 ETC 타입으로 fallback한다")
  void extractClothesByUrl_jsoupFails_returnsFallback() throws Exception {
    String url = "https://example.com/product";

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
      Connection mockConn = mock(Connection.class);
      jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(mockConn);
      given(mockConn.userAgent(anyString())).willReturn(mockConn);
      given(mockConn.timeout(anyInt())).willReturn(mockConn);
      given(mockConn.get()).willThrow(new IOException("connection refused"));

      ClothesDto result = sut.extractClothesByUrl(url);

      assertThat(result.name()).isEqualTo("알 수 없는 의상");
      assertThat(result.imageUrl()).isNull();
      assertThat(result.type()).isEqualTo(ClothesType.ETC);
      verify(chatClient, never()).prompt();
    }
  }

  @Test
  @DisplayName("AI가 null을 반환하면 타입이 ETC가 된다")
  void extractClothesByUrl_aiReturnsNull_typeIsEtc() throws Exception {
    String url = "https://example.com/product";
    String html = "<html><head>"
        + "<meta property=\"og:title\" content=\"청바지\"/>"
        + "</head></html>";
    Document doc = Jsoup.parse(html, url);

    given(chatClient.prompt()).willReturn(requestSpec);
    given(requestSpec.system(anyString())).willReturn(requestSpec);
    given(requestSpec.user(anyString())).willReturn(requestSpec);
    given(requestSpec.call()).willReturn(callResponseSpec);
    given(callResponseSpec.content()).willReturn(null);

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
      Connection mockConn = mock(Connection.class);
      jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(mockConn);
      given(mockConn.userAgent(anyString())).willReturn(mockConn);
      given(mockConn.timeout(anyInt())).willReturn(mockConn);
      given(mockConn.get()).willReturn(doc);

      ClothesDto result = sut.extractClothesByUrl(url);

      assertThat(result.name()).isEqualTo("청바지");
      assertThat(result.type()).isEqualTo(ClothesType.ETC);
    }
  }
}
