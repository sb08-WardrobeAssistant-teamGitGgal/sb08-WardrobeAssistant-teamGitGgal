package com.gitggal.clothesplz.component.batch.weather;

import com.gitggal.clothesplz.dto.notification.NotificationRequest;
import com.gitggal.clothesplz.entity.notification.NotificationLevel;
import com.gitggal.clothesplz.repository.profile.ProfileRepository;
import com.gitggal.clothesplz.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoNotificationScheduler {

    private final ProfileRepository profileRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 10 15 * * *", zone = "Asia/Seoul")
    public void sendDemoNotification() {
        profileRepository.findAll().forEach(profile -> {
            notificationService.send(new NotificationRequest(
                    profile.getUser().getId(),
                    "오늘은 산책하기 좋은 날씨입니다",
                    "맑고 쾌청한 날씨예요. 가볍게 산책해보세요!",
                    NotificationLevel.INFO
            ));
        });
        log.info("[Demo] 산책 알림 발송 완료");
    }
}
