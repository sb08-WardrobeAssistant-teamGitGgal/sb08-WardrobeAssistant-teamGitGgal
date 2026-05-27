package com.gitggal.clothesplz.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.gitggal.clothesplz.service.ai.HtmlProductExtractor.ScrapeResult;
import java.io.IOException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

@DisplayName("HtmlProductExtractor 테스트")
class HtmlProductExtractorTest {

  HtmlProductExtractor sut = new HtmlProductExtractor();

  @Test
  @DisplayName("Jsoup 조회 실패면 빈 후보와 null 이미지 반환")
  void scrape_connectionFails_returnsEmptyAndNull() throws Exception {
    String url = "https://example.com/p";
    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
      Connection conn = mock(Connection.class);
      jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(conn);
      given(conn.userAgent(anyString())).willReturn(conn);
      given(conn.timeout(anyInt())).willReturn(conn);
      given(conn.get()).willThrow(new IOException("boom"));

      ScrapeResult result = sut.scrape(url);

      assertThat(result.nameCandidates()).isEmpty();
      assertThat(result.imageUrl()).isNull();
    }
  }

  @Test
  @DisplayName("og/twitter/class/h1 이름 후보와 og:image를 추출한다")
  void scrape_extractsCandidatesAndOgImage() throws Exception {
    String url = "https://example.com/product";
    String html = """
        <html><head>
        <meta property="og:title" content="  OG 이름  "/>
        <meta name="twitter:title" content="TW 이름"/>
        <meta property="og:image" content=" https://img.example.com/a.jpg "/>
        </head>
        <body>
          <div class="GoodsName__Title">클래스 이름</div>
          <h1>H1 이름</h1>
        </body></html>
        """;
    Document doc = Jsoup.parse(html, url);

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
      Connection conn = mock(Connection.class);
      jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(conn);
      given(conn.userAgent(anyString())).willReturn(conn);
      given(conn.timeout(anyInt())).willReturn(conn);
      given(conn.get()).willReturn(doc);

      ScrapeResult result = sut.scrape(url);

      assertThat(result.nameCandidates()).containsExactly("OG 이름", "TW 이름", "클래스 이름", "H1 이름");
      assertThat(result.imageUrl()).isEqualTo("https://img.example.com/a.jpg");
    }
  }

  @Test
  @DisplayName("og:image 없으면 twitter:image, 없으면 첫 img 절대경로 fallback")
  void scrape_imageFallbackOrder() throws Exception {
    String url = "https://example.com/product";
    String html = """
        <html><head>
        <meta property="og:title" content="셔츠"/>
        <meta property="og:image" content="null"/>
        <meta name="twitter:image" content=""/>
        </head>
        <body><img src="/images/1.png"/></body></html>
        """;
    Document doc = Jsoup.parse(html, url);

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
      Connection conn = mock(Connection.class);
      jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(conn);
      given(conn.userAgent(anyString())).willReturn(conn);
      given(conn.timeout(anyInt())).willReturn(conn);
      given(conn.get()).willReturn(doc);

      ScrapeResult result = sut.scrape(url);

      assertThat(result.imageUrl()).isEqualTo("https://example.com/images/1.png");
    }
  }

  @Test
  @DisplayName("http/https 아닌 이미지 URL은 null 처리")
  void scrape_invalidImageScheme_returnsNull() throws Exception {
    String url = "https://example.com/product";
    String html = """
        <html><head>
        <meta property="og:title" content="모자"/>
        <meta property="og:image" content="javascript:alert('xss')"/>
        </head></html>
        """;
    Document doc = Jsoup.parse(html, url);

    try (MockedStatic<Jsoup> jsoupMock = mockStatic(Jsoup.class)) {
      Connection conn = mock(Connection.class);
      jsoupMock.when(() -> Jsoup.connect(url)).thenReturn(conn);
      given(conn.userAgent(anyString())).willReturn(conn);
      given(conn.timeout(anyInt())).willReturn(conn);
      given(conn.get()).willReturn(doc);

      ScrapeResult result = sut.scrape(url);

      assertThat(result.imageUrl()).isNull();
    }
  }
}
