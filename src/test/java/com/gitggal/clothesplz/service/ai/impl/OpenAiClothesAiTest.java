package com.gitggal.clothesplz.service.ai.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.dto.clothes.ClothesDto;
import com.gitggal.clothesplz.entity.clothes.Clothes;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeRepository;
import com.gitggal.clothesplz.service.ai.HtmlProductExtractor;
import com.gitggal.clothesplz.service.ai.HtmlProductExtractor.ScrapeResult;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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
  @Mock
  HtmlProductExtractor htmlExtractor;

  OpenAiClothesAi sut;
  ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    given(chatClientBuilder.build()).willReturn(chatClient);
    sut = new OpenAiClothesAi(
        chatClientBuilder,
        objectMapper,
        clothesAttributeRepository,
        htmlExtractor
    );
  }

  @Test
  @DisplayName("og:title과 og:image가 있으면 이름과 타입을 정상 추출한다")
  void extractClothesByUrl_success() throws Exception {
    String url = "https://example.com/product";
    given(htmlExtractor.scrape(url)).willReturn(
        new ScrapeResult(List.of("테스트 재킷"), "https://example.com/img.jpg"));

    given(chatClient.prompt()).willReturn(requestSpec);
    given(requestSpec.system(anyString())).willReturn(requestSpec);
    given(requestSpec.user(anyString())).willReturn(requestSpec);
    given(requestSpec.call()).willReturn(callResponseSpec);
    given(callResponseSpec.content()).willReturn("{\"type\":\"OUTER\"}");

    ClothesDto result = sut.extractClothesByUrl(url);

    assertThat(result.name()).isEqualTo("테스트 재킷");
    assertThat(result.imageUrl()).isEqualTo("https://example.com/img.jpg");
    assertThat(result.type()).isEqualTo(ClothesType.OUTER);
  }

  @Test
  @DisplayName("Jsoup 연결 실패 시 알 수 없는 의상과 ETC 타입으로 fallback한다")
  void extractClothesByUrl_jsoupFails_returnsFallback() throws Exception {
    String url = "https://example.com/product";
    given(htmlExtractor.scrape(url)).willReturn(new ScrapeResult(List.of(), null));

    ClothesDto result = sut.extractClothesByUrl(url);

    assertThat(result.name()).isEqualTo("알 수 없는 의상");
    assertThat(result.imageUrl()).isNull();
    assertThat(result.type()).isEqualTo(ClothesType.ETC);
    verify(chatClient, never()).prompt();
  }

  @Test
  @DisplayName("URL 추출 중 런타임 예외면 외곽 catch로 UNKNOWN_NAME/ETC 반환")
  void extractClothesByUrl_runtimeException_returnsFallbackFromOuterCatch() {
    String url = "https://example.com/product";
    given(htmlExtractor.scrape(url)).willThrow(new RuntimeException("boom"));

    ClothesDto result = sut.extractClothesByUrl(url);

    assertThat(result.name()).isEqualTo("알 수 없는 의상");
    assertThat(result.imageUrl()).isNull();
    assertThat(result.type()).isEqualTo(ClothesType.ETC);
  }

  @Test
  @DisplayName("AI가 null을 반환하면 타입이 ETC가 된다")
  void extractClothesByUrl_aiReturnsNull_typeIsEtc() throws Exception {
    String url = "https://example.com/product";
    given(htmlExtractor.scrape(url)).willReturn(new ScrapeResult(List.of("청바지"), null));

    given(chatClient.prompt()).willReturn(requestSpec);
    given(requestSpec.system(anyString())).willReturn(requestSpec);
    given(requestSpec.user(anyString())).willReturn(requestSpec);
    given(requestSpec.call()).willReturn(callResponseSpec);
    given(callResponseSpec.content()).willReturn(null);

    ClothesDto result = sut.extractClothesByUrl(url);

    assertThat(result.name()).isEqualTo("청바지");
    assertThat(result.type()).isEqualTo(ClothesType.ETC);
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

    List<UUID> result = sut.recommendClothesIds(weather, List.of(clothes), (short) 3);

    assertThat(result).containsExactly(clothesId);
  }

  @Test
  @DisplayName("추천 호출 예외 시 빈 목록을 반환한다")
  void recommendClothesIds_whenException_returnsEmptyList() {
    Weather weather = mock(Weather.class);
    given(chatClient.prompt()).willThrow(new RuntimeException("openai timeout"));

    List<UUID> result = sut.recommendClothesIds(weather, List.of(), (short) 3);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("og 메타가 없으면 h1과 첫 img를 fallback으로 사용한다")
  void extractClothesByUrl_fallbackToH1AndFirstImage() throws Exception {
    String url = "https://example.com/product";
    given(htmlExtractor.scrape(url)).willReturn(
        new ScrapeResult(List.of("린넨 셔츠"), "https://example.com/images/shirt.png"));

    given(chatClient.prompt()).willReturn(requestSpec);
    given(requestSpec.system(anyString())).willReturn(requestSpec);
    given(requestSpec.user(anyString())).willReturn(requestSpec);
    given(requestSpec.call()).willReturn(callResponseSpec);
    given(callResponseSpec.content()).willReturn("{\"type\":\"top\"}");

    ClothesDto result = sut.extractClothesByUrl(url);

    assertThat(result.name()).isEqualTo("린넨 셔츠");
    assertThat(result.imageUrl()).isEqualTo("https://example.com/images/shirt.png");
    assertThat(result.type()).isEqualTo(ClothesType.TOP);
  }

  @Test
  @DisplayName("이미지 URL이 http/https가 아니면 null 처리한다")
  void extractClothesByUrl_invalidImageUrl_returnsNullImage() throws Exception {
    String url = "https://example.com/product";
    given(htmlExtractor.scrape(url)).willReturn(new ScrapeResult(List.of("캡 모자"), null));

    given(chatClient.prompt()).willReturn(requestSpec);
    given(requestSpec.system(anyString())).willReturn(requestSpec);
    given(requestSpec.user(anyString())).willReturn(requestSpec);
    given(requestSpec.call()).willReturn(callResponseSpec);
    given(callResponseSpec.content()).willReturn("{\"type\":\"HAT\"}");

    ClothesDto result = sut.extractClothesByUrl(url);

    assertThat(result.name()).isEqualTo("캡 모자");
    assertThat(result.imageUrl()).isNull();
    assertThat(result.type()).isEqualTo(ClothesType.HAT);
  }

  @Test
  @DisplayName("AI 타입 응답이 비정상 값이면 ETC로 fallback한다")
  void extractClothesByUrl_invalidType_returnsEtc() throws Exception {
    String url = "https://example.com/product";
    given(htmlExtractor.scrape(url)).willReturn(new ScrapeResult(List.of("데님 팬츠"), null));

    given(chatClient.prompt()).willReturn(requestSpec);
    given(requestSpec.system(anyString())).willReturn(requestSpec);
    given(requestSpec.user(anyString())).willReturn(requestSpec);
    given(requestSpec.call()).willReturn(callResponseSpec);
    given(callResponseSpec.content()).willReturn("{\"type\":\"UNKNOWN-TYPE\"}");

    ClothesDto result = sut.extractClothesByUrl(url);

    assertThat(result.name()).isEqualTo("데님 팬츠");
    assertThat(result.type()).isEqualTo(ClothesType.ETC);
  }

  @Test
  @DisplayName("상품명 추출 실패면 UNKNOWN_NAME, 타입 ETC, AI 분류 미호출")
  void extractClothesByUrl_unknownName_skipsInferType() throws Exception {
    String url = "https://example.com/product";
    given(htmlExtractor.scrape(url)).willReturn(new ScrapeResult(List.of(), null));

    ClothesDto result = sut.extractClothesByUrl(url);

    assertThat(result.name()).isEqualTo("알 수 없는 의상");
    assertThat(result.type()).isEqualTo(ClothesType.ETC);
    verify(chatClient, never()).prompt();
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

    List<UUID> result = sut.recommendClothesIds(weather, List.of(), (short) 3);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("타입 값이 없으면 ETC fallback")
  void extractClothesByUrl_missingType_returnsEtc() throws Exception {
    String url = "https://example.com/product";
    given(htmlExtractor.scrape(url)).willReturn(new ScrapeResult(List.of("니트"), null));

    given(chatClient.prompt()).willReturn(requestSpec);
    given(requestSpec.system(anyString())).willReturn(requestSpec);
    given(requestSpec.user(anyString())).willReturn(requestSpec);
    given(requestSpec.call()).willReturn(callResponseSpec);
    given(callResponseSpec.content()).willReturn("{}");

    ClothesDto result = sut.extractClothesByUrl(url);

    assertThat(result.name()).isEqualTo("니트");
    assertThat(result.type()).isEqualTo(ClothesType.ETC);
  }

  @Test
  @DisplayName("타입 분류 응답이 비정상 JSON이면 inferType catch 후 ETC 반환")
  void extractClothesByUrl_invalidTypeJson_returnsEtcFromInferTypeCatch() throws Exception {
    String url = "https://example.com/product";
    given(htmlExtractor.scrape(url)).willReturn(new ScrapeResult(List.of("니트"), null));

    given(chatClient.prompt()).willReturn(requestSpec);
    given(requestSpec.system(anyString())).willReturn(requestSpec);
    given(requestSpec.user(anyString())).willReturn(requestSpec);
    given(requestSpec.call()).willReturn(callResponseSpec);
    given(callResponseSpec.content()).willReturn("not-json");

    ClothesDto result = sut.extractClothesByUrl(url);

    assertThat(result.name()).isEqualTo("니트");
    assertThat(result.type()).isEqualTo(ClothesType.ETC);
  }
}

