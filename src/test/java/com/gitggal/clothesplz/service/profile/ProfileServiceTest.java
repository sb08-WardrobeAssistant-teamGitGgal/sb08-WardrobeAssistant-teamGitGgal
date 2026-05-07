package com.gitggal.clothesplz.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;

import com.gitggal.clothesplz.dto.profile.common.WeatherAPILocation;
import com.gitggal.clothesplz.dto.profile.request.ProfileUpdateRequest;
import com.gitggal.clothesplz.dto.profile.response.ProfileDto;
import com.gitggal.clothesplz.entity.profile.Gender;
import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.weather.Location;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ProfileErrorCode;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.service.ServiceTestSupport;
import com.gitggal.clothesplz.service.image.ImageUploader;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DisplayName("Profile Service 테스트")
class ProfileServiceTest extends ServiceTestSupport {

  @MockitoBean
  private ImageUploader imageUploader;

  @Autowired
  private ProfileService profileService;

  private Profile profile(User user) {
    return Profile.builder()
        .user(user)
        .gender(Gender.MALE)
        .birthDate(LocalDate.of(1995, 1, 1))
        .latitude(37.5665)
        .longitude(126.9780)
        .gridX(60)
        .gridY(127)
        .build();
  }

  private Location location() {
    return Location.builder()
        .latitude(37.5665)
        .longitude(126.9780)
        .gridX(60)
        .gridY(127)
        .locationNames("서울특별시,중구")
        .build();
  }

  private ProfileUpdateRequest request() {
    return new ProfileUpdateRequest(
        "홍길동-수정",
        Gender.FEMALE,
        LocalDate.of(1998, 2, 3),
        WeatherAPILocation.of(37.5665, 126.9780, 60, 127, List.of("서울특별시", "중구")),
        5
    );
  }

  @Test
  @DisplayName("프로필 조회에 성공한다")
  void getProfile_success() {
    // given
    UUID userId = UUID.randomUUID();
    User user = new User("홍길동", "hong@test.com", "hong_password");
    Profile profile = profile(user);
    Location location = location();

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(profileRepository.findByUser(user)).willReturn(Optional.of(profile));
    given(locationRepository.findByGridXAndGridY(60, 127)).willReturn(Optional.of(location));

    // when
    ProfileDto result = profileService.getProfile(userId);

    // then
    assertThat(result.name()).isEqualTo("홍길동");
    assertThat(result.gender()).isEqualTo(Gender.MALE);
    assertThat(result.location().x()).isEqualTo(60);
    assertThat(result.location().locationNames()).contains("서울특별시");
  }

  @Test
  @DisplayName("프로필 수정에 성공한다")
  void updateProfile_success() {
    // given
    UUID userId = UUID.randomUUID();
    User user = new User("홍길동", "hong@test.com", "hong_password");
    Profile profile = profile(user);
    ProfileUpdateRequest request = request();
    Location location = location();
    MockMultipartFile image = new MockMultipartFile(
        "image",
        "profile.jpg",
        "image/jpeg",
        "img".getBytes()
    );

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(profileRepository.findByUser(user)).willReturn(Optional.of(profile));
    given(imageUploader.upload(image)).willReturn("http://localhost:8080/files/uuid.png");
    given(locationRepository.findByGridXAndGridY(60, 127)).willReturn(Optional.of(location));

    // when
    ProfileDto result = profileService.updateProfile(userId, request, image);

    // then
    assertThat(result.gender()).isEqualTo(Gender.FEMALE);
    assertThat(result.birthDate()).isEqualTo(LocalDate.of(1998, 2, 3));
    assertThat(result.temperatureSensitivity()).isEqualTo(5);
    assertThat(result.profileImageUrl()).isEqualTo("http://localhost:8080/files/uuid.png");
  }

  @Test
  @DisplayName("없는 사용자로 조회하면 USER_NOT_FOUND 예외가 발생한다")
  void getProfile_userNotFound_throwsException() {
    // given
    UUID userId = UUID.randomUUID();
    given(userRepository.findById(userId)).willReturn(Optional.empty());

    // when
    Throwable thrown = catchThrowable(() -> profileService.getProfile(userId));

    // then
    assertThat(thrown).isInstanceOf(BusinessException.class);
    BusinessException exception = (BusinessException) thrown;
    assertThat(exception.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
  }

  @Test
  @DisplayName("프로필이 없으면 PROFILE_NOT_FOUND 예외가 발생한다")
  void getProfile_profileNotFound_throwsException() {
    // given
    UUID userId = UUID.randomUUID();
    User user = new User("임꺽정", "im@test.com", "im_password");
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(profileRepository.findByUser(user)).willReturn(Optional.empty());

    // when
    Throwable thrown = catchThrowable(() -> profileService.getProfile(userId));

    // then
    assertThat(thrown).isInstanceOf(BusinessException.class);
    BusinessException exception = (BusinessException) thrown;
    assertThat(exception.getErrorCode()).isEqualTo(ProfileErrorCode.PROFILE_NOT_FOUND);
  }
}
