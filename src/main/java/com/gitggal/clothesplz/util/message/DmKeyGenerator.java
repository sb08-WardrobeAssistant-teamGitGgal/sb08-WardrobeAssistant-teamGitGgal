package com.gitggal.clothesplz.util.message;

import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * DM Key 생성 유틸
 * 
 * 두 UUID 문자열로 변환 -> 사전순 정렬한 뒤 '_'로 잇는다.
 * 같은 채널 연결 위해서 입력 순서에 상관없이 같은 결과 반영
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DmKeyGenerator {

  public static String generateKey(UUID a, UUID b) {

    Objects.requireNonNull(a, "a");

    Objects.requireNonNull(b, "b");

    String sa = a.toString();

    String sb = b.toString();

    return sa.compareTo(sb) < 0 ? sa + "_" + sb : sb + "_" + sa;
  }
}
