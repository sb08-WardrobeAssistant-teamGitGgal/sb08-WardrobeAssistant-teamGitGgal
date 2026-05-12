package com.gitggal.clothesplz.service.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

@DisplayName("Profile Service 테스트")
class ProfileServiceTest extends ServiceTestSupport {

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

  private Profile profileWithImage(User user, String imageUrl) {
    return Profile.builder()
        .user(user)
        .imageUrl(imageUrl)
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
  @DisplayName("새 이미지로 프로필 수정 시 커밋 후 기존 이미지가 삭제된다")
  void updateProfile_withNewImage_deletesOldImageAfterCommit() {
    // given
    UUID userId = UUID.randomUUID();
    User user = new User("홍길동", "hong@test.com", "hong_password");
    String oldImageUrl = "http://localhost:8080/files/old.png";
    Profile profile = profileWithImage(user, oldImageUrl);
    ProfileUpdateRequest request = request();
    Location location = location();
    MockMultipartFile image = new MockMultipartFile(
        "image",
        "profile.jpg",
        "image/jpeg",
        "img".getBytes()
    );
    String newImageUrl = "http://localhost:8080/files/new.png";

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(profileRepository.findByUser(user)).willReturn(Optional.of(profile));
    given(imageUploader.upload(image)).willReturn(newImageUrl);
    given(locationRepository.findByGridXAndGridY(60, 127)).willReturn(Optional.of(location));

    // when
    profileService.updateProfile(userId, request, image);

    // then
    verify(imageUploader).delete(oldImageUrl);
  }

  @Test
  @DisplayName("새 이미지 없이 프로필 수정 시 기존 이미지는 삭제되지 않는다")
  void updateProfile_withoutNewImage_doesNotDeleteOldImage() {
    // given
    UUID userId = UUID.randomUUID();
    User user = new User("홍길동", "hong@test.com", "hong_password");
    String oldImageUrl = "http://localhost:8080/files/old.png";
    Profile profile = profileWithImage(user, oldImageUrl);
    ProfileUpdateRequest request = request();
    Location location = location();

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(profileRepository.findByUser(user)).willReturn(Optional.of(profile));
    given(locationRepository.findByGridXAndGridY(60, 127)).willReturn(Optional.of(location));

    // when
    profileService.updateProfile(userId, request, null);

    // then
    verify(imageUploader, never()).delete(oldImageUrl);
  }

  @Test
  @DisplayName("위치 정보 없는 프로필 조회 시 location이 null로 반환된다")
  void getProfile_withNoLocation_returnsNullLocation() {
    // given
    UUID userId = UUID.randomUUID();
    User user = new User("홍길동", "hong@test.com", "hong_password");
    Profile profileNoLocation = Profile.builder()
        .user(user)
        .gender(Gender.MALE)
        .build();

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(profileRepository.findByUser(user)).willReturn(Optional.of(profileNoLocation));

    // when
    ProfileDto result = profileService.getProfile(userId);

    // then
    assertThat(result.location()).isNull();
  }

  @Test
  @DisplayName("프로필 수정 중 예외 발생 시 업로드된 이미지가 삭제된다")
  void updateProfile_onException_deletesUploadedImage() {
    // given
    UUID userId = UUID.randomUUID();
    User user = new User("홍길동", "hong@test.com", "hong_password");
    Profile profile = profile(user);
    ProfileUpdateRequest request = request();
    MockMultipartFile image = new MockMultipartFile(
        "image",
        "profile.jpg",
        "image/jpeg",
        "img".getBytes()
    );
    String newImageUrl = "http://localhost:8080/files/new.png";

    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(profileRepository.findByUser(user)).willReturn(Optional.of(profile));
    given(imageUploader.upload(image)).willReturn(newImageUrl);
    given(locationRepository.findByGridXAndGridY(60, 127))
        .willReturn(Optional.of(location()))   // findLocationOrNull 호출
        .willThrow(new RuntimeException("DB 오류"));  // updateProfile 내 두 번째 호출

    // when
    Throwable thrown = catchThrowable(() -> profileService.updateProfile(userId, request, image));

    // then
    assertThat(thrown).isInstanceOf(RuntimeException.class);
    verify(imageUploader).delete(newImageUrl);
  }

  @Test
  @DisplayName("새 이미지 URL이 기존 이미지 URL과 같으면 기존 이미지는 삭제되지 않는다")
  void updateProfile_withSameImageUrl_doesNotDeleteOldImage() {
    // given
    UUID userId = UUID.randomUUID();
    User user = new User("홍길동", "hong@test.com", "hong_password");
    String sameImageUrl = "http://localhost:8080/files/same.png";
    Profile profile = profileWithImage(user, sameImageUrl);
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
    given(imageUploader.upload(image)).willReturn(sameImageUrl);
    given(locationRepository.findByGridXAndGridY(60, 127)).willReturn(Optional.of(location));

    // when
    profileService.updateProfile(userId, request, image);

    // then
    verify(imageUploader, never()).delete(sameImageUrl);
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
