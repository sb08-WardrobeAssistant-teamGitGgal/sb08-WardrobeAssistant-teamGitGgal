package com.gitggal.clothesplz.mapper.follow;

import com.gitggal.clothesplz.dto.follow.FollowDto;
import com.gitggal.clothesplz.entity.follow.Follow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 팔로우 전용 Mapper 인터페이스
 */
@Mapper(componentModel = "spring")
public interface FollowMapper {
  
  @Mapping(source = "follower.id", target = "follower.userId")
  @Mapping(source = "followee.id", target = "followee.userId")
  @Mapping(target = "follower.profileImageUrl", ignore = true)
  @Mapping(target = "followee.profileImageUrl", ignore = true)
  FollowDto toDto(Follow follow);
}
