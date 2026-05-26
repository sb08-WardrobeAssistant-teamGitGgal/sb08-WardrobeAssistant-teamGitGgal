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
import com.gitggal.clothesplz.entity.clothes.Clothes;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeRepository;
import java.util.List;
import java.util.UUID;
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
    String html = """
        <html><head>
        <meta property="og:title" content="테스트 재킷"/>
        <meta property="og:image" content="https://example.com/img.jpg"/>
        </head></html>
        """;
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
    String html = """
        <html><head>
        <meta property="og:title" content="청바지"/>
        </head></html>
        """;
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

  @Test
  @DisplayName("추천 응답에서 잘못된 UUID는 제외하고 파싱한다")
  void recommendClothesIds_skipsInvalidUuid() {
    Weather weather = mock(Weather.class);
    given(weather.getTemperatureCurrent()).willReturn(20.0);
    given(weather.getTemperatureMin()).willReturn(18.0);
    given(weather.getTemperatureMax()).willReturn(22.0);
    given(weather.getSkyStatus()).willReturn(null);
    given(weather.getPrecipitationType()).willReturn(null);
    given(weather.getPrecipitationProbability()).willReturn(10.0);
    given(weather.getWindPhrase()).willReturn(null);
    given(weather.getHumidity()).willReturn(50.0);
    Clothes clothes = new Clothes(null, "흰 티", ClothesType.TOP, null, null);
    UUID clothesId = UUID.randomUUID();
    org.springframework.test.util.ReflectionTestUtils.setField(clothes, "id", clothesId);

    given(clothesAttributeRepository.findAllByClothesIdIn(List.of(clothesId))).willReturn(List.of());
    given(chatClient.prompt()).willReturn(requestSpec);
    given(requestSpec.system(anyString())).willReturn(requestSpec);
    given(requestSpec.user(anyString())).willReturn(requestSpec);
    given(requestSpec.call()).willReturn(callResponseSpec);
    given(callResponseSpec.content()).willReturn(
        """
            {"recommendedIds":["%s","not-a-uuid"]}
            """.formatted(clothesId)
    );

    List<UUID> result = sut.recommendClothesIds(weather, List.of(clothes));

    assertThat(result).containsExactly(clothesId);
  }

  @Test
  @DisplayName("추천 호출 예외 시 빈 목록을 반환한다")
  void recommendClothesIds_whenException_returnsEmptyList() {
    Weather weather = mock(Weather.class);
    given(chatClient.prompt()).willThrow(new RuntimeException("openai timeout"));

    List<UUID> result = sut.recommendClothesIds(weather, List.of());

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("og 메타가 없으면 h1과 첫 img를 fallback으로 사용한다")
  void extractClothesByUrl_fallbackToH1AndFirstImage() throws Exception {
    String url = "https://example.com/product";
    String html = """
        <html>
          <body>
            <h1>린넨 셔츠</h1>
            <img src="/images/shirt.png"/>
          </body>
        </html>
        """;
    Document doc = Jsoup.parse(html, url);

    given(chatClient.prompt()).willReturn(requestSpec);
    given(requestSpec.system(anyString())).willReturn(requestSpec);
    given(requestSpec.user(anyString())).willReturn(requestSpec);
    given(requestSpec.call()).willReturn(callResponseSpec);
    given(callResponseSpec.content()).willReturn("{\"type\":\"top\"}");

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
      Connection mockConn = mock(Connection.class);
      jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(mockConn);
      given(mockConn.userAgent(anyString())).willReturn(mockConn);
      given(mockConn.timeout(anyInt())).willReturn(mockConn);
      given(mockConn.get()).willReturn(doc);

      ClothesDto result = sut.extractClothesByUrl(url);

      assertThat(result.name()).isEqualTo("린넨 셔츠");
      assertThat(result.imageUrl()).isEqualTo("https://example.com/images/shirt.png");
      assertThat(result.type()).isEqualTo(ClothesType.TOP);
    }
  }

  @Test
  @DisplayName("이미지 URL이 http/https가 아니면 null 처리한다")
  void extractClothesByUrl_invalidImageUrl_returnsNullImage() throws Exception {
    String url = "https://example.com/product";
    String html = """
        <html><head>
        <meta property="og:title" content="캡 모자"/>
        <meta property="og:image" content="javascript:alert('xss')"/>
        </head></html>
        """;
    Document doc = Jsoup.parse(html, url);

    given(chatClient.prompt()).willReturn(requestSpec);
    given(requestSpec.system(anyString())).willReturn(requestSpec);
    given(requestSpec.user(anyString())).willReturn(requestSpec);
    given(requestSpec.call()).willReturn(callResponseSpec);
    given(callResponseSpec.content()).willReturn("{\"type\":\"HAT\"}");

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
      Connection mockConn = mock(Connection.class);
      jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(mockConn);
      given(mockConn.userAgent(anyString())).willReturn(mockConn);
      given(mockConn.timeout(anyInt())).willReturn(mockConn);
      given(mockConn.get()).willReturn(doc);

      ClothesDto result = sut.extractClothesByUrl(url);

      assertThat(result.name()).isEqualTo("캡 모자");
      assertThat(result.imageUrl()).isNull();
      assertThat(result.type()).isEqualTo(ClothesType.HAT);
    }
  }

  @Test
  @DisplayName("AI 타입 응답이 비정상 값이면 ETC로 fallback한다")
  void extractClothesByUrl_invalidType_returnsEtc() throws Exception {
    String url = "https://example.com/product";
    String html = """
        <html><head>
        <meta property="og:title" content="데님 팬츠"/>
        </head></html>
        """;
    Document doc = Jsoup.parse(html, url);

    given(chatClient.prompt()).willReturn(requestSpec);
    given(requestSpec.system(anyString())).willReturn(requestSpec);
    given(requestSpec.user(anyString())).willReturn(requestSpec);
    given(requestSpec.call()).willReturn(callResponseSpec);
    given(callResponseSpec.content()).willReturn("{\"type\":\"UNKNOWN-TYPE\"}");

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
      Connection mockConn = mock(Connection.class);
      jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(mockConn);
      given(mockConn.userAgent(anyString())).willReturn(mockConn);
      given(mockConn.timeout(anyInt())).willReturn(mockConn);
      given(mockConn.get()).willReturn(doc);

      ClothesDto result = sut.extractClothesByUrl(url);

      assertThat(result.name()).isEqualTo("데님 팬츠");
      assertThat(result.type()).isEqualTo(ClothesType.ETC);
    }
  }

  @Test
  @DisplayName("상품명 추출 실패면 UNKNOWN_NAME, 타입 ETC, AI 분류 미호출")
  void extractClothesByUrl_unknownName_skipsInferType() throws Exception {
    String url = "https://example.com/product";
    String html = "<html><body><div>no title here</div></body></html>";
    Document doc = Jsoup.parse(html, url);

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
      Connection mockConn = mock(Connection.class);
      jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(mockConn);
      given(mockConn.userAgent(anyString())).willReturn(mockConn);
      given(mockConn.timeout(anyInt())).willReturn(mockConn);
      given(mockConn.get()).willReturn(doc);

      ClothesDto result = sut.extractClothesByUrl(url);

      assertThat(result.name()).isEqualTo("알 수 없는 의상");
      assertThat(result.type()).isEqualTo(ClothesType.ETC);
      verify(chatClient, never()).prompt();
    }
  }

  @Test
  @DisplayName("추천 응답이 blank면 빈 리스트 반환")
  void recommendClothesIds_blankContent_returnsEmpty() {
    Weather weather = mock(Weather.class);
    given(weather.getTemperatureCurrent()).willReturn(20.0);
    given(weather.getTemperatureMin()).willReturn(18.0);
    given(weather.getTemperatureMax()).willReturn(22.0);
    given(weather.getSkyStatus()).willReturn(null);
    given(weather.getPrecipitationType()).willReturn(null);
    given(weather.getPrecipitationProbability()).willReturn(10.0);
    given(weather.getWindPhrase()).willReturn(null);
    given(weather.getHumidity()).willReturn(50.0);

    given(chatClient.prompt()).willReturn(requestSpec);
    given(requestSpec.system(anyString())).willReturn(requestSpec);
    given(requestSpec.user(anyString())).willReturn(requestSpec);
    given(requestSpec.call()).willReturn(callResponseSpec);
    given(callResponseSpec.content()).willReturn("   ");

    List<UUID> result = sut.recommendClothesIds(weather, List.of());

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("타입 값이 없으면 ETC fallback")
  void extractClothesByUrl_missingType_returnsEtc() throws Exception {
    String url = "https://example.com/product";
    String html = """
        <html><head>
        <meta property="og:title" content="니트"/>
        </head></html>
        """;
    Document doc = Jsoup.parse(html, url);

    given(chatClient.prompt()).willReturn(requestSpec);
    given(requestSpec.system(anyString())).willReturn(requestSpec);
    given(requestSpec.user(anyString())).willReturn(requestSpec);
    given(requestSpec.call()).willReturn(callResponseSpec);
    given(callResponseSpec.content()).willReturn("{}");

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
      Connection mockConn = mock(Connection.class);
      jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(mockConn);
      given(mockConn.userAgent(anyString())).willReturn(mockConn);
      given(mockConn.timeout(anyInt())).willReturn(mockConn);
      given(mockConn.get()).willReturn(doc);

      ClothesDto result = sut.extractClothesByUrl(url);

      assertThat(result.name()).isEqualTo("니트");
      assertThat(result.type()).isEqualTo(ClothesType.ETC);
    }
  }
}
