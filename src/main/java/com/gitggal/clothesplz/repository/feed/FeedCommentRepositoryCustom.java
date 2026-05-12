package com.gitggal.clothesplz.repository.feed;

import com.gitggal.clothesplz.dto.feed.CommentDto;
import com.gitggal.clothesplz.dto.feed.CommentPageRequest;
import java.util.List;
import java.util.UUID;

public interface FeedCommentRepositoryCustom {

  List<CommentDto> findAllByCursor(UUID feedId, CommentPageRequest commentPageRequest);

}
