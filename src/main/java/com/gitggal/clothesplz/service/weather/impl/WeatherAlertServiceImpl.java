package com.gitggal.clothesplz.service.weather.impl;

import com.gitggal.clothesplz.dto.notification.NotificationRequest;
import com.gitggal.clothesplz.entity.notification.NotificationLevel;
import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.entity.weather.WindPhrase;
import com.gitggal.clothesplz.repository.profile.ProfileRepository;
import com.gitggal.clothesplz.service.notification.NotificationService;
import com.gitggal.clothesplz.service.weather.WeatherAlertService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WeatherAlertServiceImpl implements WeatherAlertService {

    private final ProfileRepository profileRepository;
    private final NotificationService notificationService;
    private final EntityManager entityManager;

    @Override
    public void sendAlertsIfNeeded(Weather weather) {
        List<Profile> profiles = profileRepository.findByGridXAndGridY(
                weather.getLocation().getGridX(),
                weather.getLocation().getGridY()
        );

        if (profiles.isEmpty()) return;

        List<AlertMessage> alerts = buildAlerts(weather);
        if (alerts.isEmpty()) return;

        Instant todayStart = LocalDate.now(ZoneId.of("Asia/Seoul"))
                .atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant();

        for (Profile profile : profiles) {
            UUID userId = profile.getUser().getId();
            for (AlertMessage alert : alerts) {
                if (!alreadySentToday(userId, alert.title(), todayStart)) {
                    notificationService.send(new NotificationRequest(userId, alert.title(), alert.content(), alert.level()));
                }
            }
        }
    }

    private List<AlertMessage> buildAlerts(Weather weather) {
        List<AlertMessage> result = new ArrayList<>();

        if (weather.getPrecipitationType() != PrecipitationType.NONE
                && weather.getPrecipitationProbability() >= 60) {
            result.add(buildPrecipAlert(weather));
        }

        if (weather.getWindPhrase() == WindPhrase.STRONG) {
            result.add(new AlertMessage(
                    "오늘 강풍 예보가 있어요",
                    String.format("풍속 %.1fm/s 예상이에요. 바람막이를 챙기세요.", weather.getWindSpeed()),
                    NotificationLevel.WARNING
            ));
        }

        if (weather.getTemperatureMax() >= 33) {
            result.add(new AlertMessage(
                    "오늘 폭염 예보가 있어요",
                    String.format("최고기온 %.0f°C 예상이에요. 얇은 옷을 입으세요.", weather.getTemperatureMax()),
                    NotificationLevel.WARNING
            ));
        }

        if (weather.getTemperatureMin() <= 3) {
            result.add(new AlertMessage(
                    "오늘 한파 예보가 있어요",
                    String.format("최저기온 %.0f°C 예상이에요. 따뜻하게 입으세요.", weather.getTemperatureMin()),
                    NotificationLevel.WARNING
            ));
        }

        double swing = weather.getTemperatureMax() - weather.getTemperatureMin();
        if (swing >= 10) {
            result.add(new AlertMessage(
                    "오늘 일교차가 크게 납니다",
                    String.format("기온 차이가 %.0f°C예요. 겉옷을 꼭 챙기세요.", swing),
                    NotificationLevel.INFO
            ));
        }

        return result;
    }

    private AlertMessage buildPrecipAlert(Weather weather) {
        return switch (weather.getPrecipitationType()) {
            case RAIN -> new AlertMessage(
                    "오늘 비 예보가 있어요",
                    String.format("강수 확률 %.0f%%예요. 우산을 챙기세요.", weather.getPrecipitationProbability()),
                    NotificationLevel.WARNING
            );
            case SNOW -> new AlertMessage(
                    "오늘 눈 예보가 있어요",
                    "눈이 올 수 있어요. 따뜻하게 입고 미끄럼 조심하세요.",
                    NotificationLevel.WARNING
            );
            case RAIN_SNOW -> new AlertMessage(
                    "오늘 비/눈 예보가 있어요",
                    "강수 예보가 있어요. 우산과 따뜻한 옷을 챙기세요.",
                    NotificationLevel.WARNING
            );
            case SHOWER -> new AlertMessage(
                    "오늘 소나기 예보가 있어요",
                    "소나기가 올 수 있어요. 가벼운 우산을 챙기세요.",
                    NotificationLevel.WARNING
            );
            default -> throw new IllegalStateException("Unhandled precipitation type: " + weather.getPrecipitationType());
        };
    }

    private boolean alreadySentToday(UUID userId, String title, Instant after) {
        Long count = entityManager.createQuery(
                        "SELECT COUNT(n) FROM Notification n WHERE n.receiver.id = :userId AND n.title = :title AND n.createdAt > :after",
                        Long.class)
                .setParameter("userId", userId)
                .setParameter("title", title)
                .setParameter("after", after)
                .getSingleResult();
        return count > 0;
    }

    private record AlertMessage(String title, String content, NotificationLevel level) {}
}
