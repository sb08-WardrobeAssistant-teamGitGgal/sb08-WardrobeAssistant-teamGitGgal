package com.gitggal.clothesplz.dto.feed;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record FeedCreateRequest(
    @NotNull(message = "피드 작성자가 비어있을 수 없습니다.")
    UUID authorId,

    @NotNull(message = "날씨가 비어있을 수 없습니다.")
    UUID weatherId,

    @NotNull @NotEmpty(message = "추천 의상 목록이 비어있을 수 없습니다.")
    List<@NotNull UUID> clothesIds,

    @NotBlank(message = "피드 내용이 비어있을 수 없습니다.")
    @Size(max = 2000, message = "피드 내용은 2000자를 초과할 수 없습니다.")
    String content
) {

}
