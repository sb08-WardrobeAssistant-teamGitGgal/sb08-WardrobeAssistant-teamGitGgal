package com.gitggal.clothesplz.service.weather;

import com.gitggal.clothesplz.dto.notification.NotificationRequest;
import com.gitggal.clothesplz.entity.notification.NotificationLevel;
import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.entity.weather.Location;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.entity.weather.WindPhrase;
import com.gitggal.clothesplz.repository.profile.ProfileRepository;
import com.gitggal.clothesplz.service.notification.NotificationService;
import com.gitggal.clothesplz.service.weather.impl.WeatherAlertServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("WeatherAlertServiceImpl 단위 테스트")
@ExtendWith(MockitoExtension.class)
class WeatherAlertServiceImplTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EntityManager entityManager;

    @SuppressWarnings("rawtypes")
    @Mock
    private TypedQuery typedQuery;

    @InjectMocks
    private WeatherAlertServiceImpl alertService;

    private Location location;
    private Profile profile;
    private UUID userId;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(userId);

        location = mock(Location.class);
        lenient().when(location.getGridX()).thenReturn(60);
        lenient().when(location.getGridY()).thenReturn(127);

        profile = mock(Profile.class);
        lenient().when(profile.getUser()).thenReturn(user);

        lenient().when(entityManager.createQuery(anyString(), eq(Object[].class))).thenReturn(typedQuery);
        lenient().when(typedQuery.setParameter(anyString(), any())).thenReturn(typedQuery);
        lenient().when(typedQuery.getResultList()).thenReturn(List.of());
    }

    @Nested
    @DisplayName("해당 위치에 유저가 없을 때")
    class NoUsersAtLocation {

        @Test
        @DisplayName("알림 미발송")
        void sendAlertsIfNeeded_noProfiles_sendsNothing() {
            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of());
            Weather weather = makeWeather(PrecipitationType.RAIN, 70.0, WindPhrase.WEAK, 20.0, 12.0, 3.0);

            alertService.sendAlertsIfNeeded(weather);

            verifyNoInteractions(notificationService);
        }
    }

    @Nested
    @DisplayName("강수 조건")
    class PrecipitationAlert {

        @Test
        @DisplayName("비 + 강수확률 60% 이상 → WARNING 알림")
        void rain_highProbability_sendsWarning() {
            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile));
            Weather weather = makeWeather(PrecipitationType.RAIN, 70.0, WindPhrase.WEAK, 20.0, 12.0, 3.0);

            alertService.sendAlertsIfNeeded(weather);

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).send(captor.capture());
            NotificationRequest req = captor.getValue();
            assertThat(req.receiverId()).isEqualTo(userId);
            assertThat(req.title()).isEqualTo("오늘 비 예보가 있어요");
            assertThat(req.content()).contains("70%");
            assertThat(req.level()).isEqualTo(NotificationLevel.WARNING);
        }

        @Test
        @DisplayName("비 + 강수확률 정확히 60% → WARNING 알림")
        void rain_exactBoundaryProbability_sendsWarning() {
            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile));
            Weather weather = makeWeather(PrecipitationType.RAIN, 60.0, WindPhrase.WEAK, 20.0, 12.0, 3.0);

            alertService.sendAlertsIfNeeded(weather);

            verify(notificationService).send(any(NotificationRequest.class));
        }

        @Test
        @DisplayName("비 + 강수확률 59% 이하 → 알림 없음")
        void rain_lowProbability_sendsNothing() {
            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile));
            Weather weather = makeWeather(PrecipitationType.RAIN, 59.0, WindPhrase.WEAK, 20.0, 12.0, 3.0);

            alertService.sendAlertsIfNeeded(weather);

            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("눈 예보 → WARNING 알림")
        void snow_sendsWarning() {
            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile));
            Weather weather = makeWeather(PrecipitationType.SNOW, 70.0, WindPhrase.WEAK, 20.0, 12.0, 3.0);

            alertService.sendAlertsIfNeeded(weather);

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).send(captor.capture());
            assertThat(captor.getValue().title()).isEqualTo("오늘 눈 예보가 있어요");
        }

        @Test
        @DisplayName("비/눈 예보 → WARNING 알림")
        void rainSnow_sendsWarning() {
            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile));
            Weather weather = makeWeather(PrecipitationType.RAIN_SNOW, 70.0, WindPhrase.WEAK, 20.0, 12.0, 3.0);

            alertService.sendAlertsIfNeeded(weather);

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).send(captor.capture());
            assertThat(captor.getValue().title()).isEqualTo("오늘 비/눈 예보가 있어요");
        }

        @Test
        @DisplayName("소나기 예보 → WARNING 알림")
        void shower_sendsWarning() {
            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile));
            Weather weather = makeWeather(PrecipitationType.SHOWER, 70.0, WindPhrase.WEAK, 20.0, 12.0, 3.0);

            alertService.sendAlertsIfNeeded(weather);

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).send(captor.capture());
            assertThat(captor.getValue().title()).isEqualTo("오늘 소나기 예보가 있어요");
        }

        @Test
        @DisplayName("강수 없음 → 알림 없음")
        void noPrecip_sendsNothing() {
            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile));
            Weather weather = makeWeather(PrecipitationType.NONE, 0.0, WindPhrase.WEAK, 20.0, 12.0, 3.0);

            alertService.sendAlertsIfNeeded(weather);

            verifyNoInteractions(notificationService);
        }
    }

    @Nested
    @DisplayName("강풍 조건")
    class WindAlert {

        @Test
        @DisplayName("STRONG 풍속 → WARNING 알림")
        void strongWind_sendsWarning() {
            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile));
            Weather weather = makeWeather(PrecipitationType.NONE, 0.0, WindPhrase.STRONG, 20.0, 12.0, 10.0);

            alertService.sendAlertsIfNeeded(weather);

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).send(captor.capture());
            assertThat(captor.getValue().title()).isEqualTo("오늘 강풍 예보가 있어요");
            assertThat(captor.getValue().level()).isEqualTo(NotificationLevel.WARNING);
            assertThat(captor.getValue().content()).contains("10.0m/s");
        }

        @Test
        @DisplayName("MODERATE 풍속 → 알림 없음")
        void moderateWind_sendsNothing() {
            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile));
            Weather weather = makeWeather(PrecipitationType.NONE, 0.0, WindPhrase.MODERATE, 20.0, 12.0, 5.0);

            alertService.sendAlertsIfNeeded(weather);

            verifyNoInteractions(notificationService);
        }
    }

    @Nested
    @DisplayName("극한 기온 조건")
    class ExtremeTemperatureAlert {

        @Test
        @DisplayName("최고기온 33°C 이상 → 폭염 WARNING")
        void heatwave_sendsWarning() {
            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile));
            Weather weather = makeWeather(PrecipitationType.NONE, 0.0, WindPhrase.WEAK, 33.0, 25.0, 3.0);

            alertService.sendAlertsIfNeeded(weather);

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).send(captor.capture());
            assertThat(captor.getValue().title()).isEqualTo("오늘 폭염 예보가 있어요");
            assertThat(captor.getValue().level()).isEqualTo(NotificationLevel.WARNING);
        }

        @Test
        @DisplayName("최고기온 32°C → 알림 없음")
        void belowHeatwaveThreshold_sendsNothing() {
            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile));
            Weather weather = makeWeather(PrecipitationType.NONE, 0.0, WindPhrase.WEAK, 32.0, 24.0, 3.0);

            alertService.sendAlertsIfNeeded(weather);

            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("최저기온 3°C 이하 → 한파 WARNING")
        void coldWave_sendsWarning() {
            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile));
            Weather weather = makeWeather(PrecipitationType.NONE, 0.0, WindPhrase.WEAK, 9.0, 0.0, 3.0);

            alertService.sendAlertsIfNeeded(weather);

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).send(captor.capture());
            assertThat(captor.getValue().title()).isEqualTo("오늘 한파 예보가 있어요");
            assertThat(captor.getValue().level()).isEqualTo(NotificationLevel.WARNING);
        }

        @Test
        @DisplayName("최저기온 정확히 3°C → 한파 WARNING")
        void coldWave_exactBoundary_sendsWarning() {
            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile));
            Weather weather = makeWeather(PrecipitationType.NONE, 0.0, WindPhrase.WEAK, 9.0, 3.0, 3.0);

            alertService.sendAlertsIfNeeded(weather);

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).send(captor.capture());
            assertThat(captor.getValue().title()).isEqualTo("오늘 한파 예보가 있어요");
        }

        @Test
        @DisplayName("최저기온 4°C → 알림 없음")
        void aboveColdWaveThreshold_sendsNothing() {
            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile));
            Weather weather = makeWeather(PrecipitationType.NONE, 0.0, WindPhrase.WEAK, 10.0, 4.0, 3.0);

            alertService.sendAlertsIfNeeded(weather);

            verifyNoInteractions(notificationService);
        }
    }

    @Nested
    @DisplayName("큰 일교차 조건")
    class TemperatureSwingAlert {

        @Test
        @DisplayName("일교차 12°C 이상 → INFO 알림")
        void bigSwing_sendsInfo() {
            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile));
            Weather weather = makeWeather(PrecipitationType.NONE, 0.0, WindPhrase.WEAK, 25.0, 13.0, 3.0);

            alertService.sendAlertsIfNeeded(weather);

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).send(captor.capture());
            assertThat(captor.getValue().title()).isEqualTo("오늘 일교차가 크게 납니다");
            assertThat(captor.getValue().level()).isEqualTo(NotificationLevel.INFO);
            assertThat(captor.getValue().content()).contains("12°C");
        }

        @Test
        @DisplayName("일교차 정확히 10°C → INFO 알림")
        void swing_exactBoundary_sendsInfo() {
            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile));
            Weather weather = makeWeather(PrecipitationType.NONE, 0.0, WindPhrase.WEAK, 20.0, 10.0, 3.0);

            alertService.sendAlertsIfNeeded(weather);

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).send(captor.capture());
            assertThat(captor.getValue().title()).isEqualTo("오늘 일교차가 크게 납니다");
            assertThat(captor.getValue().level()).isEqualTo(NotificationLevel.INFO);
        }

        @Test
        @DisplayName("일교차 9°C → 알림 없음")
        void smallSwing_sendsNothing() {
            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile));
            Weather weather = makeWeather(PrecipitationType.NONE, 0.0, WindPhrase.WEAK, 19.0, 10.0, 3.0);

            alertService.sendAlertsIfNeeded(weather);

            verifyNoInteractions(notificationService);
        }
    }

    @Nested
    @DisplayName("복합 조건")
    class MultipleAlerts {

        @Test
        @DisplayName("비 + 강풍 동시 → 유저 1명에게 알림 2건")
        void rainAndWind_sendsMultipleAlerts() {
            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile));
            Weather weather = makeWeather(PrecipitationType.RAIN, 70.0, WindPhrase.STRONG, 20.0, 12.0, 10.0);

            alertService.sendAlertsIfNeeded(weather);

            verify(notificationService, times(2)).send(any(NotificationRequest.class));
        }

        @Test
        @DisplayName("유저 2명 + 알림 조건 1개 → 각각 알림 발송")
        void twoUsers_eachReceivesAlert() {
            UUID userId2 = UUID.randomUUID();
            User user2 = mock(User.class);
            given(user2.getId()).willReturn(userId2);
            Profile profile2 = mock(Profile.class);
            given(profile2.getUser()).willReturn(user2);

            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile, profile2));
            Weather weather = makeWeather(PrecipitationType.RAIN, 70.0, WindPhrase.WEAK, 20.0, 12.0, 3.0);

            alertService.sendAlertsIfNeeded(weather);

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService, times(2)).send(captor.capture());
            List<UUID> receiverIds = captor.getAllValues().stream().map(NotificationRequest::receiverId).toList();
            assertThat(receiverIds).containsExactlyInAnyOrder(userId, userId2);
        }
    }

    @Nested
    @DisplayName("중복 알림 방지")
    class DuplicateAlertPrevention {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("오늘 이미 같은 제목 알림 발송됨 → 중복 미발송")
        void alreadySentToday_doesNotSendDuplicate() {
            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile));
            given(typedQuery.getResultList()).willReturn(Collections.singletonList(new Object[]{userId, "오늘 비 예보가 있어요"}));
            Weather weather = makeWeather(PrecipitationType.RAIN, 70.0, WindPhrase.WEAK, 20.0, 12.0, 3.0);

            alertService.sendAlertsIfNeeded(weather);

            verifyNoInteractions(notificationService);
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("유저 2명 중 1명만 이미 발송 → 나머지 1명에게만 발송")
        void oneAlreadySent_sendsToOtherOnly() {
            UUID userId2 = UUID.randomUUID();
            User user2 = mock(User.class);
            given(user2.getId()).willReturn(userId2);
            Profile profile2 = mock(Profile.class);
            given(profile2.getUser()).willReturn(user2);

            given(profileRepository.findByGridXAndGridY(any(), any())).willReturn(List.of(profile, profile2));
            // userId는 이미 발송, userId2는 미발송
            given(typedQuery.getResultList()).willReturn(Collections.singletonList(new Object[]{userId, "오늘 비 예보가 있어요"}));
            Weather weather = makeWeather(PrecipitationType.RAIN, 70.0, WindPhrase.WEAK, 20.0, 12.0, 3.0);

            alertService.sendAlertsIfNeeded(weather);

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService, times(1)).send(captor.capture());
            assertThat(captor.getValue().receiverId()).isEqualTo(userId2);
        }
    }

    private Weather makeWeather(
            PrecipitationType precipType,
            double precipProb,
            WindPhrase windPhrase,
            double tempMax,
            double tempMin,
            double windSpeed
    ) {
        Weather weather = mock(Weather.class);
        lenient().when(weather.getLocation()).thenReturn(location);
        lenient().when(weather.getPrecipitationType()).thenReturn(precipType);
        lenient().when(weather.getPrecipitationProbability()).thenReturn(precipProb);
        lenient().when(weather.getWindPhrase()).thenReturn(windPhrase);
        lenient().when(weather.getTemperatureMax()).thenReturn(tempMax);
        lenient().when(weather.getTemperatureMin()).thenReturn(tempMin);
        lenient().when(weather.getWindSpeed()).thenReturn(windSpeed);
        return weather;
    }
}
