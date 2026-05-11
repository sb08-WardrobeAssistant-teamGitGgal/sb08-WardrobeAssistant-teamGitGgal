package com.gitggal.clothesplz.service;

import com.gitggal.clothesplz.repository.clothes.ClothesAttributeDefRepository;
import com.gitggal.clothesplz.repository.clothes.ClothesAttributeRepository;
import com.gitggal.clothesplz.repository.clothes.ClothesRepository;
import com.gitggal.clothesplz.repository.feed.FeedCommentRepository;
import com.gitggal.clothesplz.repository.feed.FeedLikeRepository;
import com.gitggal.clothesplz.repository.feed.FeedRepository;
import com.gitggal.clothesplz.repository.follow.FollowRepository;
import com.gitggal.clothesplz.repository.message.DirectMessageRepository;
import com.gitggal.clothesplz.repository.notification.NotificationRepository;
import com.gitggal.clothesplz.repository.profile.ProfileRepository;
import com.gitggal.clothesplz.repository.user.SocialAccountRepository;
import com.gitggal.clothesplz.repository.user.UserRepository;
import com.gitggal.clothesplz.repository.weather.LocationRepository;
import com.gitggal.clothesplz.repository.weather.WeatherRepository;
import com.gitggal.clothesplz.service.image.ImageUploader;
import com.gitggal.clothesplz.service.weather.WeatherApiService;
import com.gitggal.clothesplz.support.IntegrationTestSupport;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

public abstract class ServiceTestSupport extends IntegrationTestSupport {

  @MockitoBean
  protected UserRepository userRepository;
  @MockitoBean
  protected SocialAccountRepository socialAccountRepository;
  @MockitoBean
  protected ProfileRepository profileRepository;
  @MockitoBean
  protected FollowRepository followRepository;
  @MockitoBean
  protected DirectMessageRepository directMessageRepository;
  @MockitoBean
  protected NotificationRepository notificationRepository;
  @MockitoBean
  protected FeedRepository feedRepository;
  @MockitoBean
  protected FeedLikeRepository feedLikeRepository;
  @MockitoBean
  protected FeedCommentRepository feedCommentRepository;
  @MockitoBean
  protected ClothesRepository clothesRepository;
  @MockitoBean
  protected ClothesAttributeRepository clothesAttributeRepository;
  @MockitoBean
  protected ClothesAttributeDefRepository clothesAttributeDefRepository;
  @MockitoBean
  protected WeatherRepository weatherRepository;
  @MockitoBean
  protected LocationRepository locationRepository;
  @MockitoBean
  protected WeatherApiService weatherApiService;
}
