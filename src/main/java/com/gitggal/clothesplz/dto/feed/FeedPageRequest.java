package com.gitggal.clothesplz.dto.feed;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record FeedPageRequest(
    String cursor,

    UUID idAfter,

    @NotNull(message = "limit은 필수입니다.")
    @Min(value = 1, message = "limit은 1 이상이어야 합니다.")
    Integer limit,

    @NotBlank(message = "정렬 기준은 필수입니다.")
    @Pattern(regexp = "createdAt|likeCount")
    @Schema(allowableValues = {"createdAt", "likeCount"}, defaultValue = "createdAt")
    String sortBy,

    @NotBlank(message = "정렬 방향은 필수입니다.")
    @Pattern(regexp = "ASCENDING|DESCENDING")
    @Schema(allowableValues = {"ASCENDING", "DESCENDING"}, defaultValue = "ASCENDING")
    String sortDirection,

    String keywordLike,

    @Pattern(regexp = "CLEAR|MOSTLY_CLOUDY|CLOUDY")
    @Schema(allowableValues = {"CLEAR", "MOSTLY_CLOUDY", "CLOUDY"})
    String skyStatusEqual,

    @Pattern(regexp = "NONE|RAIN|RAIN_SNOW|SNOW|SHOWER")
    @Schema(allowableValues = {"NONE", "RAIN", "RAIN_SNOW", "SNOW", "SHOWER"})
    String precipitationTypeEqual,

    UUID authorIdEqual
) {

}
