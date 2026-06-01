package com.gitggal.clothesplz.mapper.follow;

import com.gitggal.clothesplz.dto.follow.FollowDto;
import com.gitggal.clothesplz.dto.follow.UserSummary;
import com.gitggal.clothesplz.entity.follow.Follow;
import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.repository.profile.ProfileRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 팔로우 전용 Mapper 인터페이스
 */
@Mapper(componentModel = "spring")
public abstract class FollowMapper {

  @Autowired
  private ProfileRepository profileRepository;
  
  @Mapping(source = "follower", target = "follower", qualifiedByName = "toUserSummary")
  @Mapping(source = "followee", target = "followee", qualifiedByName = "toUserSummary")
  public abstract FollowDto toDto(Follow follow);

  @Named("toUserSummary")
  protected UserSummary toUserSummary(User user) {

    if (user == null) return null;

    String profileImageUrl = profileRepository.findByUserId(user.getId())
        .map(Profile::getImageUrl)
        .orElse(null);

    return new UserSummary(user.getId(), user.getName(), profileImageUrl);
  }

}
