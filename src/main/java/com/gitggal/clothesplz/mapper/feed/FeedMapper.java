package com.gitggal.clothesplz.mapper.feed;

import com.gitggal.clothesplz.dto.feed.FeedDto;
import com.gitggal.clothesplz.dto.user.AuthorDto;
import com.gitggal.clothesplz.dto.weather.PrecipitationDto;
import com.gitggal.clothesplz.dto.weather.TemperatureDto;
import com.gitggal.clothesplz.dto.weather.WeatherSummaryDto;
import com.gitggal.clothesplz.entity.feed.Feed;
import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.repository.profile.ProfileRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class FeedMapper {

  @Autowired
  private ProfileRepository profileRepository;

  @Mapping(target = "author", source = "author")
  @Mapping(target = "weather", source = "weather")
  @Mapping(target = "likedByMe", expression = "java(false)")
  abstract public FeedDto toDto(Feed feed);

  protected AuthorDto toAuthorDto(User user) {
    String profileImageUrl = profileRepository.findByUser(user)
        .map(Profile::getImageUrl)
        .orElse(null);

    return new AuthorDto(user.getId(), user.getName(), profileImageUrl);
  }

  protected WeatherSummaryDto toWeatherSummaryDto(Weather weather) {
    return WeatherSummaryDto.builder()
        .weatherId(weather.getId())
        .skyStatus(weather.getSkyStatus())
        .precipitation(PrecipitationDto.builder()
            .type(weather.getPrecipitationType())
            .amount(weather.getPrecipitationAmount())
            .probability(weather.getPrecipitationProbability())
            .build())
        .temperature(TemperatureDto.builder()
            .current(weather.getTemperatureCurrent())
            .comparedToDayBefore(weather.getTemperatureDiff())
            .min(weather.getTemperatureMin())
            .max(weather.getTemperatureMax())
            .build())
        .build();
  }
}
