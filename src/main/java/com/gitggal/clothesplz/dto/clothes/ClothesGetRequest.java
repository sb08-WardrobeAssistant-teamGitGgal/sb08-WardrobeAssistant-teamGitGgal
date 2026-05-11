package com.gitggal.clothesplz.dto.clothes;

import com.gitggal.clothesplz.entity.clothes.ClothesType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ClothesGetRequest(
    @Schema(description = "커서 (이전 응답의 nextCursor 값)", example = "2024-01-01T00:00:00Z")
    String cursor,

    @Schema(description = "커서 동점 처리용 ID (이전 응답의 nextIdAfter 값)")
    UUID idAfter,

    @Min(1)
    @Max(100)
    @NotNull(message = "조회 갯수는 필수 입니다.")
    @Schema(description = "조회 갯수", defaultValue = "20", minimum = "1", maximum = "100")
    Integer limit,

    @Schema(description = "의상 종류 필터 (TOP, BOTTOM, DRESS, OUTER 등)")
    ClothesType typeEqual,

    @NotNull(message = "소유자의 ID는 필수입니다.")
    @Schema(description = "소유자 사용자 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID ownerId
) {

  @AssertTrue(message = "cursor와 idAfter는 함께 제공되거나 함께 생략되어야 합니다.")
  public boolean isCursorConsistent() {
    return (cursor == null) == (idAfter == null);
  }
}
