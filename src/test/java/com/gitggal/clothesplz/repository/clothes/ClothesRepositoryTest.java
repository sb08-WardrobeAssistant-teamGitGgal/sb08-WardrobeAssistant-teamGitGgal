package com.gitggal.clothesplz.repository.clothes;

import static org.assertj.core.api.Assertions.assertThat;

import com.gitggal.clothesplz.config.QuerydslConfig;
import com.gitggal.clothesplz.dto.clothes.ClothesGetRequest;
import com.gitggal.clothesplz.entity.clothes.Clothes;
import com.gitggal.clothesplz.entity.clothes.ClothesType;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.repository.RepositoryTestSupport;
import java.time.Instant;
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

  private Clothes persistClothes(User owner, String name, ClothesType type) {
    Clothes clothes = new Clothes(owner, name, type, null, null);
    return em.persistAndFlush(clothes);
  }

  private void waitForNextTimestamp() {
    try {
      Thread.sleep(10);
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
  @DisplayName("cursor가 있으면 cursor 이전 데이터만 조회한다")
  void findAllByCursor_withCursor_returnsOlderItems() {
    User owner = em.persistAndFlush(new User("owner2", "owner2@test.com", "pw"));

    Clothes c1 = persistClothes(owner, "a", ClothesType.TOP);
    waitForNextTimestamp();
    Clothes c2 = persistClothes(owner, "b", ClothesType.TOP);
    waitForNextTimestamp();
    persistClothes(owner, "c", ClothesType.TOP);
    em.clear();

    Instant cursor = c2.getCreatedAt();

    ClothesGetRequest request = new ClothesGetRequest(
        cursor.toString(),
        UUID.fromString("00000000-0000-0000-0000-000000000000"),
        20,
        null,
        owner.getId()
    );

    List<Clothes> result = clothesRepository.findAllByCursor(request, cursor);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo(c1.getId());
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
}
