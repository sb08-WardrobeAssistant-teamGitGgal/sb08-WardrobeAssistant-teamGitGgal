package com.gitggal.clothesplz.component.batch.weather;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@DisplayName("WeatherBatchScheduler 단위 테스트")
@ExtendWith(MockitoExtension.class)
class WeatherBatchSchedulerTest {

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job weatherFetchJob;

    @InjectMocks
    private WeatherBatchScheduler scheduler;

    @Test
    @DisplayName("정상 실행 → jobLauncher.run() 호출")
    void runWeatherFetchJob_success_launchesJob() throws Exception {
        scheduler.runWeatherFetchJob();

        verify(jobLauncher).run(eq(weatherFetchJob), any(JobParameters.class));
    }

    @Test
    @DisplayName("JobParameters에 timestamp 포함")
    void runWeatherFetchJob_containsTimestampParameter() throws Exception {
        ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);

        scheduler.runWeatherFetchJob();

        verify(jobLauncher).run(any(), captor.capture());
        assertThat(captor.getValue().getLong("timestamp")).isNotNull();
    }

    @Test
    @DisplayName("jobLauncher 예외 발생 → 예외 삼킴, 외부로 전파 안 됨")
    void runWeatherFetchJob_exception_doesNotPropagate() throws Exception {
        willThrow(new RuntimeException("KMA API 오류"))
                .given(jobLauncher).run(any(), any());

        assertThatNoException().isThrownBy(() -> scheduler.runWeatherFetchJob());
    }
}
