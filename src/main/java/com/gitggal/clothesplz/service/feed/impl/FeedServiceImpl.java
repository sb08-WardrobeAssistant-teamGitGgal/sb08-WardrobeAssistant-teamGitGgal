package com.gitggal.clothesplz.service.feed.impl;

import com.gitggal.clothesplz.dto.clothes.OotdDto;
import com.gitggal.clothesplz.dto.feed.FeedCreateRequest;
import com.gitggal.clothesplz.dto.feed.FeedDto;
import com.gitggal.clothesplz.entity.feed.Feed;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.exception.code.WeatherErrorCode;
import com.gitggal.clothesplz.mapper.feed.FeedMapper;
import com.gitggal.clothesplz.repository.feed.FeedRepository;
import com.gitggal.clothesplz.repository.user.UserRepository;
import com.gitggal.clothesplz.repository.weather.WeatherRepository;
import com.gitggal.clothesplz.service.feed.FeedService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class FeedServiceImpl implements FeedService {

  private final UserRepository userRepository;
  private final WeatherRepository weatherRepository;
  private final FeedRepository feedRepository;
  private final FeedMapper feedMapper;

  @Override
  @Transactional
  public FeedDto createFeed(FeedCreateRequest feedCreateRequest) {
    log.info("[Service] 피드 생성 요청 시작 - authorId: {}, weatherId: {}",
        feedCreateRequest.authorId(), feedCreateRequest.weatherId());
    UUID weatherId = feedCreateRequest.weatherId();
    UUID authorId = feedCreateRequest.authorId();
    List<UUID> clothesId = feedCreateRequest.clothesIds();
    String content = feedCreateRequest.content();

    Weather weather = weatherRepository.findById(weatherId)
        .orElseThrow(() -> new BusinessException(WeatherErrorCode.WEATHER_NOT_FOUND));

    User author = userRepository.findById(authorId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    // TODO: clothesService 구현 후 교체
    List<OotdDto> ootds = List.of();

    Feed feed = new Feed(weather, author, ootds, content);

    Feed savedFeed = feedRepository.save(feed);

    log.info("[Service] 피드 생성 요청 완료 - feedId: {}", savedFeed.getId());

    return feedMapper.toDto(savedFeed);
  }
}
