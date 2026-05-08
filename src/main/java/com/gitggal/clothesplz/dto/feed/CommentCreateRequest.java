package com.gitggal.clothesplz.dto.feed;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CommentCreateRequest(
    // 현재 사용하지 않음(pathVariable로 feedId 사용)
    // 하지만, swagger 설계와 맞추기 위해 추가한 상태
    @NotNull(message = "피드 정보가 비어있을 수 없습니다.")
    UUID feedId,

    @NotNull(message = "댓글 작성자가 비어있을 수 없습니다.")
    UUID authorId,

    @NotBlank(message = "댓글 내용이 비어있을 수 없습니다.")
    @Size(max = 2000, message = "댓글 내용은 2000자를 초과할 수 없습니다.")
    String content
) {

}
