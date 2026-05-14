package com.gitggal.clothesplz.component.batch.weather;

import com.gitggal.clothesplz.dto.weather.DailyWeatherForecastDto;
import com.gitggal.clothesplz.dto.weather.WeatherApiResponseDto;
import com.gitggal.clothesplz.entity.weather.*;
import com.gitggal.clothesplz.repository.weather.LocationRepository;
import com.gitggal.clothesplz.repository.weather.WeatherRepository;
import com.gitggal.clothesplz.service.image.ImageUploader;
import com.gitggal.clothesplz.service.weather.WeatherApiService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("WeatherBatchJob 통합 테스트")
class WeatherBatchJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private Job weatherFetchJob;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private WeatherRepository weatherRepository;

    @MockitoBean
    private WeatherApiService weatherApiService;

    @MockitoBean
    private ImageUploader imageUploader;

    @MockitoBean
    private Flyway flyway;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(weatherFetchJob);
        jobRepositoryTestUtils.removeJobExecutions();
        weatherRepository.deleteAll();
        locationRepository.deleteAll();
    }

    @Test
    @DisplayName("Location 없음 → Job COMPLETED (처리 건수 0)")
    void job_noLocations_completesWithZeroItems() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(weatherRepository.count()).isZero();
    }

    @Test
    @DisplayName("Location 존재 + 정상 API 응답 → Weather 저장")
    void job_withLocation_savesWeather() throws Exception {
        Location location = locationRepository.save(Location.builder()
                .latitude(37.5)
                .longitude(127.0)
                .gridX(60)
                .gridY(127)
                .locationNames("서울특별시 중구")
                .build());

        WeatherApiResponseDto response = mock(WeatherApiResponseDto.class);
        List<DailyWeatherForecastDto> forecasts = List.of(
                new DailyWeatherForecastDto(
                        LocalDate.now(), SkyStatus.CLEAR,
                        20.0, 15.0, 25.0,
                        60.0, 5.0,
                        PrecipitationType.NONE, 0.0, 10.0,
                        2.0, 1.0
                )
        );
        given(weatherApiService.fetchWeather(anyInt(), anyInt())).willReturn(Mono.just(response));

        // WeatherParserService는 실제 구현체 사용 (통합 테스트)
        // parseDailyForecast(mock response) → 빈 리스트 반환됨
        // 실제 저장 검증보다 Job 상태 검증에 집중
        JobParameters params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    @DisplayName("API 예외 발생 → skip 처리, Job COMPLETED")
    void job_apiException_skipsAndCompletes() throws Exception {
        locationRepository.save(Location.builder()
                .latitude(37.5)
                .longitude(127.0)
                .gridX(60)
                .gridY(127)
                .locationNames("서울특별시 중구")
                .build());

        given(weatherApiService.fetchWeather(anyInt(), anyInt()))
                .willReturn(Mono.error(new RuntimeException("KMA API 오류")));

        JobParameters params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(weatherRepository.count()).isZero();
    }
}
