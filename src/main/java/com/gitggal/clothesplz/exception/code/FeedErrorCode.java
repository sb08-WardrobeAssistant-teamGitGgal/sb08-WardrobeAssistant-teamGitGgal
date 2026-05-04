package com.gitggal.clothesplz.exception.code;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum FeedErrorCode implements ErrorCode {

  FEED_NOT_FOUND(2001, HttpStatus.NOT_FOUND, "피드를 찾을 수 없습니다."),
  FEED_COMMENT_NOT_FOUND(2002, HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
  FEED_LIKE_ALREADY_EXISTS(2003, HttpStatus.CONFLICT, "이미 좋아요한 피드입니다."),
  FEED_LIKE_NOT_FOUND(2004, HttpStatus.NOT_FOUND, "좋아요 내역을 찾을 수 없습니다."),
  UNAUTHORIZED_FEED_ACCESS(2005, HttpStatus.FORBIDDEN, "해당 피드에 접근 권한이 없습니다.");

  private final int code;
  private final HttpStatus httpStatus;
  private final String message;

  @Override
  public int getCode() { return code; }

  @Override
  public HttpStatus getHttpStatus() { return httpStatus; }

  @Override
  public String getMessage() { return message; }
}
