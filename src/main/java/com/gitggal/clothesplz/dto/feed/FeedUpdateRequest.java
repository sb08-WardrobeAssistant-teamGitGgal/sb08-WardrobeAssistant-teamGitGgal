package com.gitggal.clothesplz.dto.feed;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeedUpdateRequest(
    @NotBlank(message = "피드 내용이 비어있을 수 없습니다.")
    @Size(max = 2000, message = "피드 내용은 2000자를 초과할 수 없습니다.")
    String content
) {

}
