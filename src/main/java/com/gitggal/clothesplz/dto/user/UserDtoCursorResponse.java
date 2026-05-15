package com.gitggal.clothesplz.dto.user;

import java.util.List;
import java.util.UUID;

public record UserDtoCursorResponse(
    List<UserDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    int totalCount,
    String sortBy,
    String sortDirection
) {

}
