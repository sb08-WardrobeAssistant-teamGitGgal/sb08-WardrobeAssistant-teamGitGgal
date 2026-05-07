package com.gitggal.clothesplz.service.feed;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.gitggal.clothesplz.dto.feed.FeedCreateRequest;
import com.gitggal.clothesplz.dto.feed.FeedDto;
import com.gitggal.clothesplz.entity.feed.Feed;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.mapper.feed.FeedMapper;
import com.gitggal.clothesplz.service.ServiceTestSupport;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ExtendWith(MockitoExtension.class)
@DisplayName("피드 서비스 테스트")
public class FeedServiceTest extends ServiceTestSupport {

  @Autowired
  private FeedService feedService;

  @MockitoBean
  private FeedMapper feedMapper;

  private UUID weatherId;
  private UUID authorId;
  private Weather mockWeather;
  private User mockAuthor;
  private FeedCreateRequest feedCreateRequest;

  @BeforeEach
  void setUp() {
    weatherId = UUID.randomUUID();
    authorId = UUID.randomUUID();
    mockWeather = mock(Weather.class);
    mockAuthor = mock(User.class);
    feedCreateRequest = new FeedCreateRequest(
        authorId,
        weatherId,
        List.of(UUID.randomUUID()),
        "피드 생성"
    );
  }

  @Nested
  @DisplayName("피드 생성 관련 테스트")
  class createFeedTests {

    @Test
    @DisplayName("피드 생성 성공인 경우")
    void createFeed_success() {
      // given
      given(weatherRepository.findById(eq(weatherId))).willReturn(Optional.of(mockWeather));
      given(userRepository.findById(authorId)).willReturn(Optional.of(mockAuthor));
      given(feedRepository.save(any(Feed.class))).willAnswer(inv -> inv.getArgument(0));

      FeedDto expectedDto = mock(FeedDto.class);
      given(feedMapper.toDto(any(Feed.class))).willReturn(expectedDto);

      // when
      FeedDto result = feedService.createFeed(feedCreateRequest);

      // then
      assertThat(result).isEqualTo(expectedDto);
      then(feedRepository).should().save(any(Feed.class));
      then(feedMapper).should().toDto(any(Feed.class));
    }

    @Test
    @DisplayName("날씨 정보를 찾을 수 없는 경우 예외 발생")
    void createFeed_weatherNotFound_ThrowsException() {
      // given
      given(weatherRepository.findById(eq(weatherId))).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> feedService.createFeed(feedCreateRequest))
          .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("유저 정보를 찾을 수 없는 경우 예외 발생")
    void createFeed_userNotFound_ThrowsException() {
      // given
      given(weatherRepository.findById(eq(weatherId))).willReturn(Optional.of(mockWeather));
      given(userRepository.findById(eq(authorId))).willReturn(Optional.empty());

      // when & then
      assertThatThrownBy(() -> feedService.createFeed(feedCreateRequest))
          .isInstanceOf(BusinessException.class);
    }
  }
}
