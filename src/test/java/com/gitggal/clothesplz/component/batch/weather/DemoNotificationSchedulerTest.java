package com.gitggal.clothesplz.component.batch.weather;

import com.gitggal.clothesplz.dto.notification.NotificationRequest;
import com.gitggal.clothesplz.entity.notification.NotificationLevel;
import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.repository.profile.ProfileRepository;
import com.gitggal.clothesplz.service.notification.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("DemoNotificationScheduler 단위 테스트")
@ExtendWith(MockitoExtension.class)
class DemoNotificationSchedulerTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DemoNotificationScheduler scheduler;

    @Test
    @DisplayName("전체 유저에게 산책 알림 발송")
    void sendDemoNotification_sendsToAllProfiles() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        Profile p1 = mockProfile(userId1);
        Profile p2 = mockProfile(userId2);
        given(profileRepository.findAll()).willReturn(List.of(p1, p2));

        scheduler.sendDemoNotification();

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        verify(notificationService, times(2)).send(captor.capture());

        List<NotificationRequest> sent = captor.getAllValues();
        assertThat(sent).extracting(NotificationRequest::receiverId)
                .containsExactlyInAnyOrder(userId1, userId2);
        assertThat(sent).allSatisfy(req -> {
            assertThat(req.title()).isEqualTo("오늘은 산책하기 좋은 날씨입니다");
            assertThat(req.level()).isEqualTo(NotificationLevel.INFO);
        });
    }

    @Test
    @DisplayName("프로필 없으면 알림 미발송")
    void sendDemoNotification_noProfiles_sendsNothing() {
        given(profileRepository.findAll()).willReturn(List.of());

        scheduler.sendDemoNotification();

        verifyNoInteractions(notificationService);
    }

    private Profile mockProfile(UUID userId) {
        User user = mock(User.class);
        given(user.getId()).willReturn(userId);
        Profile profile = mock(Profile.class);
        given(profile.getUser()).willReturn(user);
        return profile;
    }
}
