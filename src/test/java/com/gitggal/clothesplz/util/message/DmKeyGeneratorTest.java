package com.gitggal.clothesplz.util.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DM Key 생성 유틸 테스트")
public class DmKeyGeneratorTest {

  @Test
  @DisplayName("두 UUID 입력 순서가 달라도 같은 키를 만든다.")
  void generate_dmKey_IsSymmetric() {

    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();

    String key1 = DmKeyGenerator.generateKey(a, b);
    String key2 = DmKeyGenerator.generateKey(b, a);

    assertThat(key1).isEqualTo(key2);
  }

  @Test
  @DisplayName("키는 사전순으로 정렬된 두 UUID를 '_'로 잇는다")
  void generate_dmKey_LexicalOrder() {

    UUID a = UUID.fromString("293e6307-3493-41da-98f5-22e32b968abe");
    UUID b = UUID.fromString("75f3dc83-88fc-4f3d-9fd9-857646d3f50a");

    String key = DmKeyGenerator.generateKey(a, b);

    assertThat(key).isEqualTo(a + "_" + b);
  }
}
