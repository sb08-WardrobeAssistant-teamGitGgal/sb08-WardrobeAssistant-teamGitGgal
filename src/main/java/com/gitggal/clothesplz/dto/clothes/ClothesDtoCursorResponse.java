package com.gitggal.clothesplz.dto.clothes;

import java.util.List;
import java.util.UUID;

public record ClothesDtoCursorResponse(
    List<ClothesDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    Long totalCount,
    String sortBy,
    String sortDirection
) {

}
