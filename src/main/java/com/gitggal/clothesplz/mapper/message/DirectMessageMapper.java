package com.gitggal.clothesplz.mapper.message;

import com.gitggal.clothesplz.dto.follow.UserSummary;
import com.gitggal.clothesplz.dto.message.DirectMessageDto;
import com.gitggal.clothesplz.entity.message.DirectMessage;
import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.repository.profile.ProfileRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * DM 전용 Mapper 인터페이스
 */
@Mapper(componentModel = "spring")
public abstract class DirectMessageMapper {

  @Autowired
  protected ProfileRepository profileRepository;

  @Mapping(source = "sender", target = "sender", qualifiedByName = "toUserSummary")
  @Mapping(source = "receiver", target = "receiver", qualifiedByName = "toUserSummary")
  public abstract DirectMessageDto toDto(DirectMessage directMessage);

  @Named("toUserSummary")
  protected UserSummary toUserSummary(User user) {

    if (user == null) return null;

    String profileImageUrl = profileRepository.findByUserId(user.getId())
        .map(Profile::getImageUrl)
        .orElse(null);

    return new UserSummary(user.getId(), user.getName(), profileImageUrl);
  }
}
