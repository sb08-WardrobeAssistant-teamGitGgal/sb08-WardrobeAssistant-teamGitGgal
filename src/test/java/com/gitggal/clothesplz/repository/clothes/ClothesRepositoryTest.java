package com.gitggal.clothesplz.repository.clothes;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitggal.clothesplz.config.QuerydslConfig;
import com.gitggal.clothesplz.dto.clothes.ClothesGetRequest;
import com.gitggal.clothesplz.entity.clothes.ClothesAttributeDef;
import com.gitggal.clothesplz.entity.clothes.Clothes;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.repository.RepositoryTestSupport;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Import(QuerydslConfig.class)
@EnableJpaAuditing
@DisplayName("Clothes Repository 테스트")
class ClothesRepositoryTest extends RepositoryTestSupport {

  @Autowired
  private TestEntityManager em;

  @Autowired
  private ClothesRepository clothesRepository;
  @Autowired
  private ClothesAttributeDefRepository clothesAttributeDefRepository;

  private Clothes persistClothes(User owner, String name, ClothesType type) {
    Clothes clothes = new Clothes(owner, name, type, null, null);
    return em.persistAndFlush(clothes);
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
  @DisplayName("커서 없이 조회하면 소유자 기준 createdAt/id 내림차순으로 limit+1 조회한다")
  void findAllByCursor_withoutCursor_returnsSortedByCreatedAtDesc() {
    User owner = em.persistAndFlush(new User("owner", "owner@test.com", "pw"));
    User other = em.persistAndFlush(new User("other", "other@test.com", "pw"));

    Clothes c1 = persistClothes(owner, "a", ClothesType.TOP);
    waitForNextTimestamp();
    Clothes c2 = persistClothes(owner, "b", ClothesType.TOP);
    waitForNextTimestamp();
    Clothes c3 = persistClothes(owner, "c", ClothesType.TOP);
    persistClothes(other, "x", ClothesType.TOP);
    em.clear();

    ClothesGetRequest request = new ClothesGetRequest(null, null, 2, null, owner.getId());

    List<Clothes> result = clothesRepository.findAllByCursor(request, null);

    assertThat(result).hasSize(3);
    assertThat(result.get(0).getId()).isEqualTo(c3.getId());
    assertThat(result.get(1).getId()).isEqualTo(c2.getId());
    assertThat(result.get(2).getId()).isEqualTo(c1.getId());
    assertThat(result).allMatch(c -> c.getOwner().getId().equals(owner.getId()));
  }

  @Test
  @DisplayName("countByCursor는 owner/type 필터를 반영한 총 개수를 반환한다")
  void countByCursor_withTypeFilter_returnsFilteredCount() {
    User owner = em.persistAndFlush(new User("owner3", "owner3@test.com", "pw"));

    persistClothes(owner, "top-1", ClothesType.TOP);
    persistClothes(owner, "top-2", ClothesType.TOP);
    persistClothes(owner, "bottom-1", ClothesType.BOTTOM);

    ClothesGetRequest request = new ClothesGetRequest(null, null, 20, ClothesType.TOP, owner.getId());

    Long result = clothesRepository.countByCursor(request);

    assertThat(result).isEqualTo(2L);
  }

  @Test
  @DisplayName("existsByIdAndOwnerId는 의상과 소유자가 일치하면 true를 반환한다")
  void existsByIdAndOwnerId_returnsTrueWhenOwnerMatches() {
    User owner = em.persistAndFlush(new User("owner4", "owner4@test.com", "pw"));
    User other = em.persistAndFlush(new User("other4", "other4@test.com", "pw"));
    Clothes clothes = persistClothes(owner, "coat", ClothesType.OUTER);

    boolean matched = clothesRepository.existsByIdAndOwnerId(clothes.getId(), owner.getId());
    boolean unmatched = clothesRepository.existsByIdAndOwnerId(clothes.getId(), other.getId());

    assertThat(matched).isTrue();
    assertThat(unmatched).isFalse();
  }

  @Test
  @DisplayName("findByOwnerId는 해당 소유자의 의상만 반환한다")
  void findByOwnerId_returnsOnlyOwnersClothes() {
    User owner = em.persistAndFlush(new User("owner6", "owner6@test.com", "pw"));
    User other = em.persistAndFlush(new User("other6", "other6@test.com", "pw"));

    persistClothes(owner, "owner-top", ClothesType.TOP);
    persistClothes(owner, "owner-bottom", ClothesType.BOTTOM);
    persistClothes(other, "other-outer", ClothesType.OUTER);
    em.clear();

    List<Clothes> result = clothesRepository.findByOwnerId(owner.getId());

    assertThat(result).hasSize(2);
    assertThat(result).allMatch(c -> c.getOwner().getId().equals(owner.getId()));
    assertThat(result).extracting(Clothes::getName)
        .containsExactlyInAnyOrder("owner-top", "owner-bottom");
  }

  @Test
  @DisplayName("의상 삭제 후에는 existsByIdAndOwnerId가 false를 반환한다")
  void existsByIdAndOwnerId_returnsFalseAfterDelete() {
    User owner = em.persistAndFlush(new User("owner5", "owner5@test.com", "pw"));
    Clothes clothes = persistClothes(owner, "target", ClothesType.TOP);
    UUID clothesId = clothes.getId();
    UUID ownerId = owner.getId();

    clothesRepository.delete(clothes);
    em.flush();
    em.clear();

    assertThat(clothesRepository.existsByIdAndOwnerId(clothesId, ownerId)).isFalse();
  }

  @Test
  @DisplayName("existsByName은 같은 이름의 의상 속성 정의가 존재하면 true를 반환한다")
  void existsByName_returnsTrueWhenExists() {
    clothesAttributeDefRepository.save(
        new ClothesAttributeDef("색상", List.of("WHITE", "BLACK"))
    );
    em.flush();
    em.clear();

    boolean result = clothesAttributeDefRepository.existsByName("색상");

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("existsByName은 같은 이름의 의상 속성 정의가 없으면 false를 반환한다")
  void existsByName_returnsFalseWhenNotExists() {
    clothesAttributeDefRepository.save(
        new ClothesAttributeDef("소재", List.of("COTTON", "WOOL"))
    );
    em.flush();
    em.clear();

    boolean result = clothesAttributeDefRepository.existsByName("색상");

    assertThat(result).isFalse();
  }
}
