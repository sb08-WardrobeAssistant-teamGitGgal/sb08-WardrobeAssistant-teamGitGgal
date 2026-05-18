package com.gitggal.clothesplz.mapper.message;

import com.gitggal.clothesplz.dto.follow.UserSummary;
import com.gitggal.clothesplz.dto.message.DirectMessageDto;
import com.gitggal.clothesplz.entity.message.DirectMessage;
import com.gitggal.clothesplz.entity.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * DM 전용 Mapper 인터페이스
 */
@Mapper(componentModel = "spring")
public interface DirectMessageMapper {

  @Mapping(source = "sender", target = "sender", qualifiedByName = "toUserSummary")
  @Mapping(source = "receiver", target = "receiver", qualifiedByName = "toUserSummary")
  DirectMessageDto toDto(DirectMessage directMessage);

  @Named("toUserSummary")
  default UserSummary toUserSummary(User user) {

    if (user == null) return null;

    return new UserSummary(user.getId(), user.getName(), null);
  }
}
