package com.gitggal.clothesplz.mapper.profile;

import com.gitggal.clothesplz.dto.profile.common.WeatherAPILocation;
import com.gitggal.clothesplz.dto.profile.response.ProfileDto;
import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProfileMapper {

  @Mapping(target = "userId", source = "user.id")
  @Mapping(target = "name", source = "user.name")
  @Mapping(target = "gender", source = "profile.gender")
  @Mapping(target = "birthDate", source = "profile.birthDate")
  @Mapping(target = "location", source = "location")
  @Mapping(target = "temperatureSensitivity", source = "profile.tempSensitivity")
  @Mapping(target = "profileImageUrl", source = "profile.imageUrl")
  ProfileDto toDto(User user, Profile profile, WeatherAPILocation location);
}
