package com.gitggal.clothesplz.service.ai;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HtmlProductExtractor {

  // 내부용 record
  public record ScrapeResult(
      List<String> nameCandidates,
      String imageUrl
  ) {

  }

  public ScrapeResult scrape(String url) {
    Document doc;
    try {
      doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(5000).get();

    } catch (IOException e) {
      log.warn("[HTML] 상품 페이지 조회 실패.");
      return new ScrapeResult(List.of(), null);
    }
    return new ScrapeResult(
        collectNameCandidates(doc),
        validateImageUrl(collectImageUrl(doc))
    );
  }

  // 의상 이름 추출 -> 보통 쇼핑몰 meta태그에 적힘
  private List<String> collectNameCandidates(Document doc) {
    List<String> candidates = new ArrayList<>();
    addIfPresent(candidates, doc.select("meta[property=og:title]").attr("content"));
    addIfPresent(candidates, doc.select("meta[name=twitter:title]").attr("content"));
    addIfPresent(candidates, doc.select("[class*=GoodsName__]").text());
    addIfPresent(candidates, doc.select("h1").text());
    return candidates;
  }

  private void addIfPresent(List<String> list, String value) {
    if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) {
      list.add(value.trim());
    }
  }

  // 의상 이미지 추출 -> 보통 쇼핑몰 첫 번쨰 img태그에 이미지가 대표 이미지
  private String collectImageUrl(Document doc) {
    for (String candidate : List.of(
        doc.select("meta[property=og:image]").attr("content"),
        doc.select("meta[name=twitter:image]").attr("content")
    )) {
      if (!candidate.isBlank() && !"null".equalsIgnoreCase(candidate)) {
        return candidate.trim();
      }
    }
    Element img = doc.selectFirst("img");
    return img == null ? null : img.absUrl("src");
  }

  private String validateImageUrl(String imageUrl) {
    if (imageUrl == null || imageUrl.isBlank()) {
      return null;
    }
    return imageUrl.startsWith("http://") || imageUrl.startsWith("https://") ? imageUrl : null;
  }
}
