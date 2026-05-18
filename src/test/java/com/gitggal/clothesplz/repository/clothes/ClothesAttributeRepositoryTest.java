package com.gitggal.clothesplz.repository.clothes;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitggal.clothesplz.config.QuerydslConfig;
import com.gitggal.clothesplz.entity.clothes.Clothes;
import com.gitggal.clothesplz.entity.clothes.ClothesAttribute;
import com.gitggal.clothesplz.entity.clothes.ClothesAttributeDef;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.repository.RepositoryTestSupport;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Import(QuerydslConfig.class)
@EnableJpaAuditing
@DisplayName("ClothesAttribute Repository 테스트")
class ClothesAttributeRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private ClothesAttributeRepository repository;

  private User owner;
  private Clothes clothes;

  @BeforeEach
  void setUp() {
    owner = em.persistAndFlush(new User("홍길동", "hong@test.com", "pw"));
    clothes = em.persistAndFlush(new Clothes(owner, "티셔츠", ClothesType.TOP, null, null));
  }

  private ClothesAttributeDef persistDef(String name, String... values) {
    return em.persistAndFlush(new ClothesAttributeDef(name, List.of(values)));
  }

  private ClothesAttribute persistAttribute(Clothes c, ClothesAttributeDef def, String value) {
    return em.persistAndFlush(
        ClothesAttribute.builder().clothes(c).definition(def).value(value).build()
    );
  }

  @Test
  @DisplayName("해당 definition의 ClothesAttribute를 모두 삭제한다")
  void deleteAllByDefinitionId_deletesMatchingAttributes() {
    ClothesAttributeDef def = persistDef("색상", "WHITE");
    persistAttribute(clothes, def, "WHITE");
    em.clear();

    repository.deleteAllByDefinitionId(def.getId());

    List<ClothesAttribute> remaining = repository.findAllByClothesIdIn(List.of(clothes.getId()));
    assertThat(remaining).isEmpty();
  }

  @Test
  @DisplayName("다른 definition의 ClothesAttribute는 삭제하지 않는다")
  void deleteAllByDefinitionId_doesNotDeleteOtherDefinitionsAttributes() {
    ClothesAttributeDef targetDef = persistDef("색상", "WHITE");
    ClothesAttributeDef otherDef = persistDef("소재", "COTTON");
    persistAttribute(clothes, targetDef, "WHITE");
    persistAttribute(clothes, otherDef, "COTTON");
    em.clear();

    repository.deleteAllByDefinitionId(targetDef.getId());

    List<ClothesAttribute> remaining = repository.findAllByClothesIdIn(List.of(clothes.getId()));
    assertThat(remaining).hasSize(1);
    assertThat(remaining.get(0).getDefinition().getId()).isEqualTo(otherDef.getId());
  }
}
