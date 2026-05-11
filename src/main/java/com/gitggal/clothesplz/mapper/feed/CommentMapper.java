package com.gitggal.clothesplz.mapper.feed;

import com.gitggal.clothesplz.dto.feed.CommentDto;
import com.gitggal.clothesplz.entity.feed.FeedComment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

// TODO: authorMapper 구현되면 교체 예정
//@Mapper(componentModel = "spring", uses = {AuthorMapper.class})
@Mapper(componentModel = "spring", uses = {FeedMapper.class})
public interface CommentMapper {

  @Mapping(target = "feedId", source = "feed.id")
  CommentDto toDto(FeedComment comment);
}
