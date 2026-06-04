package com.gitggal.clothesplz.mapper.message;

import com.gitggal.clothesplz.dto.follow.UserSummary;
import com.gitggal.clothesplz.dto.message.DirectMessageDto;
import com.gitggal.clothesplz.entity.message.DirectMessage;
import com.gitggal.clothesplz.entity.user.User;
import java.util.Map;
import java.util.UUID;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public abstract class DirectMessageMapper {

  @Mapping(source = "sender", target = "sender", qualifiedByName = "toUserSummary")
  @Mapping(source = "receiver", target = "receiver", qualifiedByName = "toUserSummary")
  public abstract DirectMessageDto toDto(DirectMessage directMessage, @Context Map<UUID, String> imageByUserId);

  @Named("toUserSummary")
  protected UserSummary toUserSummary(User user, @Context Map<UUID, String> imageByUserId) {
    if (user == null) return null;
    return new UserSummary(user.getId(), user.getName(), imageByUserId.get(user.getId()));
  }
}
