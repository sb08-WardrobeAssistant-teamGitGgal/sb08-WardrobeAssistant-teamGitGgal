package com.gitggal.clothesplz.service.feed;

import com.gitggal.clothesplz.dto.feed.CommentCreateRequest;
import com.gitggal.clothesplz.dto.feed.CommentDto;
import com.gitggal.clothesplz.dto.feed.FeedCreateRequest;
import com.gitggal.clothesplz.dto.feed.FeedDto;
import com.gitggal.clothesplz.dto.feed.FeedUpdateRequest;
import java.util.UUID;

public interface FeedService {

  // 피드 등록
  FeedDto createFeed(FeedCreateRequest feedCreateRequest);

  // 피드 수정
  FeedDto updateFeed(UUID feedId, FeedUpdateRequest feedUpdateRequest);

  // 피드 삭제
  void deleteFeed(UUID feedId);

  // 피드 좋아요
  void increaseLikeCount(UUID feedId, UUID userId);

  // 피드 좋아요 취소
  void decreaseLikeCount(UUID feedId, UUID userId);

  // 댓글 등록
  CommentDto createComment(UUID feedId ,CommentCreateRequest commentCreateRequest);

  // 댓글 목록 조회

  // 피드 목록 조회
}
