package com.gitggal.clothesplz.repository.clothes;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitggal.clothesplz.config.QuerydslConfig;
import com.gitggal.clothesplz.entity.clothes.ClothesAttributeDef;
import com.gitggal.clothesplz.repository.RepositoryTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Import(QuerydslConfig.class)
@EnableJpaAuditing
@DisplayName("ClothesAttributeDef Repository 테스트")
class ClothesAttributeDefRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private ClothesAttributeDefRepository repository;

  private ClothesAttributeDef persist(String name, String... values) {
    ClothesAttributeDef def = new ClothesAttributeDef(name, List.of(values));
    return em.persistAndFlush(def);
  }

  private void waitForNextTimestamp() {
    try {
      Thread.sleep(1100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  @Test
  @DisplayName("sortBy=name, sortDirection=ASCENDING이면 이름 오름차순으로 반환한다")
  void findAllByConditions_sortByNameAscending_returnsNameAsc() {
    persist("소재", "COTTON");
    persist("색상", "WHITE");
    persist("핏", "SLIM");
    em.clear();

    List<ClothesAttributeDef> result = repository.findAllByConditions("name", "ASCENDING", null);

    assertThat(result).extracting(ClothesAttributeDef::getName)
        .containsExactly("색상", "소재", "핏");
  }

  @Test
  @DisplayName("sortBy=name, sortDirection=DESCENDING이면 이름 내림차순으로 반환한다")
  void findAllByConditions_sortByNameDescending_returnsNameDesc() {
    persist("소재", "COTTON");
    persist("색상", "WHITE");
    persist("핏", "SLIM");
    em.clear();

    List<ClothesAttributeDef> result = repository.findAllByConditions("name", "DESCENDING", null);

    assertThat(result).extracting(ClothesAttributeDef::getName)
        .containsExactly("핏", "소재", "색상");
  }

  @Test
  @DisplayName("sortBy=createdAt이면 생성일 내림차순으로 반환한다")
  void findAllByConditions_sortByCreatedAtDescending_returnsCreatedAtDesc() {
    persist("색상", "WHITE");
    waitForNextTimestamp();
    persist("소재", "COTTON");
    em.clear();

    List<ClothesAttributeDef> result = repository.findAllByConditions("createdAt", "DESCENDING", null);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).getName()).isEqualTo("소재");
    assertThat(result.get(1).getName()).isEqualTo("색상");
  }

  @Test
  @DisplayName("keywordLike가 있으면 이름에 포함된 항목만 반환한다")
  void findAllByConditions_withKeyword_returnsFiltered() {
    persist("색상", "WHITE");
    persist("소재", "COTTON");
    persist("색감", "VIVID");
    em.clear();

    List<ClothesAttributeDef> result = repository.findAllByConditions("name", "ASCENDING", "색");

    assertThat(result).hasSize(2);
    assertThat(result).extracting(ClothesAttributeDef::getName)
        .containsExactlyInAnyOrder("색상", "색감");
  }

  @Test
  @DisplayName("keywordLike가 대소문자 구분 없이 필터링한다")
  void findAllByConditions_keywordCaseInsensitive_returnsFiltered() {
    persist("Cotton", "COTTON");
    persist("소재", "WOOL");
    em.clear();

    List<ClothesAttributeDef> result = repository.findAllByConditions("name", "ASCENDING", "cotton");

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getName()).isEqualTo("Cotton");
  }

  @Test
  @DisplayName("keywordLike가 null이면 전체 목록을 반환한다")
  void findAllByConditions_keywordNull_returnsAll() {
    persist("색상", "WHITE");
    persist("소재", "COTTON");
    em.clear();

    List<ClothesAttributeDef> result = repository.findAllByConditions("name", "ASCENDING", null);

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("keywordLike가 공백이면 전체 목록을 반환한다")
  void findAllByConditions_keywordBlank_returnsAll() {
    persist("색상", "WHITE");
    persist("소재", "COTTON");
    em.clear();

    List<ClothesAttributeDef> result = repository.findAllByConditions("name", "ASCENDING", "  ");

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("일치하는 키워드가 없으면 빈 리스트를 반환한다")
  void findAllByConditions_noMatch_returnsEmpty() {
    persist("색상", "WHITE");
    em.clear();

    List<ClothesAttributeDef> result = repository.findAllByConditions("name", "ASCENDING", "없는키워드");

    assertThat(result).isEmpty();
  }
}
