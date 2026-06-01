package com.gitggal.clothesplz.mapper.follow;

import com.gitggal.clothesplz.dto.follow.FollowDto;
import com.gitggal.clothesplz.dto.follow.UserSummary;
import com.gitggal.clothesplz.entity.follow.Follow;
import com.gitggal.clothesplz.entity.user.User;
import java.util.Map;
import java.util.UUID;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public abstract class FollowMapper {

  @Mapping(source = "follower", target = "follower", qualifiedByName = "toUserSummary")
  @Mapping(source = "followee", target = "followee", qualifiedByName = "toUserSummary")
  public abstract FollowDto toDto(Follow follow, @Context Map<UUID, String> imageByUserId);

  @Named("toUserSummary")
  protected UserSummary toUserSummary(User user, @Context Map<UUID, String> imageByUserId) {
    if (user == null) return null;
    return new UserSummary(user.getId(), user.getName(), imageByUserId.get(user.getId()));
  }

}
