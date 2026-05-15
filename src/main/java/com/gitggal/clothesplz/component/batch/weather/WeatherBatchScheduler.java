package com.gitggal.clothesplz.component.batch.weather;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 기상청 발표 시각(02,05,08,11,14,17,20,23시) 30분 후 배치 자동 실행
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job weatherFetchJob;

    // 기상청 단기예보 발표 시각(02, 05, 08, 11, 14, 17, 20, 23시) 기준 30분 후 실행
    @Scheduled(cron = "0 30 2,5,8,11,14,17,20,23 * * *", zone = "Asia/Seoul")
    public void runWeatherFetchJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(weatherFetchJob, params);
            log.info("[Scheduler] 날씨 배치 실행 완료");
        } catch (Exception e) {
            log.error("[Scheduler] 날씨 배치 실행 실패: {}", e.getMessage());
        }
    }
}
