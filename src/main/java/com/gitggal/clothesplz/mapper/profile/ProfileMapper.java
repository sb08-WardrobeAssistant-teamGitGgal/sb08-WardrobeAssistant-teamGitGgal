package com.gitggal.clothesplz.mapper.profile;

import com.gitggal.clothesplz.dto.profile.common.WeatherAPILocation;
import com.gitggal.clothesplz.dto.profile.response.ProfileDto;
import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.weather.Location;
import java.util.Arrays;
import java.util.List;
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
  ProfileDto toProfileDto(User user, Profile profile, WeatherAPILocation location);


  @Mapping(target = "latitude", source = "location.latitude")
  @Mapping(target = "longitude", source = "location.longitude")
  @Mapping(target = "x", source = "location.gridX")
  @Mapping(target = "y", source = "location.gridY")
  @Mapping(target = "locationNames", source = "location.locationNames")
  WeatherAPILocation toWeatherAPILocation(Location location);

  default List<String> toLocationNamesList(String locationNames) {
    if (locationNames == null) {
      return List.of();
    }

    return Arrays.stream(locationNames.split(","))
        .map(String::trim)
        .toList();
  }
}
