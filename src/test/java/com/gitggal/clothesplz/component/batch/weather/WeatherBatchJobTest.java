package com.gitggal.clothesplz.component.batch.weather;

import com.gitggal.clothesplz.dto.weather.DailyWeatherForecastDto;
import com.gitggal.clothesplz.dto.weather.WeatherApiResponseDto;
import com.gitggal.clothesplz.entity.weather.*;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.WeatherErrorCode;
import com.gitggal.clothesplz.repository.weather.LocationRepository;
import com.gitggal.clothesplz.repository.weather.WeatherRepository;
import com.gitggal.clothesplz.service.image.ImageUploader;
import com.gitggal.clothesplz.service.weather.WeatherApiService;
import com.gitggal.clothesplz.service.weather.WeatherParserService;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("WeatherBatchJob 통합 테스트")
class WeatherBatchJobTest {

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

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
    private WeatherParserService weatherParserService;

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

        // 기본 파서 응답: 빈 리스트 (각 테스트에서 필요 시 override)
        given(weatherParserService.parseDailyForecast(any())).willReturn(List.of());
    }

    @Test
    @DisplayName("Location 없음 → Job COMPLETED (처리 건수 0)")
    void job_noLocations_completesWithZeroItems() throws Exception {
        JobExecution execution = jobLauncherTestUtils.launchJob(buildParams());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(weatherRepository.count()).isZero();
    }

    @Test
    @DisplayName("Location 존재 + 정상 API 응답 → Job COMPLETED")
    void job_withLocation_completesSuccessfully() throws Exception {
        saveLocation(60, 127);
        given(weatherApiService.fetchWeather(anyInt(), anyInt()))
                .willReturn(Mono.just(emptyResponse()));

        JobExecution execution = jobLauncherTestUtils.launchJob(buildParams());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    @DisplayName("BusinessException 발생 → skip 처리, Job COMPLETED")
    void job_businessException_skipsAndCompletes() throws Exception {
        saveLocation(60, 127);
        given(weatherApiService.fetchWeather(anyInt(), anyInt()))
                .willReturn(Mono.error(new BusinessException(WeatherErrorCode.WEATHER_API_ERROR)));

        JobExecution execution = jobLauncherTestUtils.launchJob(buildParams());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(weatherRepository.count()).isZero();
    }

    @Test
    @DisplayName("동일 forecastAt 중복 요청 → 1건만 저장")
    void job_duplicateData_skipsExisting() throws Exception {
        Location location = saveLocation(60, 127);

        OffsetDateTime forecastAt = LocalDate.now().atStartOfDay().atOffset(KST);

        // 기존 데이터 저장
        weatherRepository.save(Weather.builder()
                .forecastedAt(OffsetDateTime.now(KST))
                .forecastAt(forecastAt)
                .location(location)
                .skyStatus(SkyStatus.CLEAR)
                .precipitationType(PrecipitationType.NONE)
                .precipitationAmount(0.0)
                .precipitationProbability(0.0)
                .humidity(60.0)
                .humidityDiff(0.0)
                .temperatureCurrent(20.0)
                .temperatureDiff(0.0)
                .temperatureMin(15.0)
                .temperatureMax(25.0)
                .windSpeed(2.0)
                .windPhrase(WindPhrase.WEAK)
                .build());

        // 배치가 동일 forecastAt 반환
        given(weatherApiService.fetchWeather(anyInt(), anyInt()))
                .willReturn(Mono.just(emptyResponse()));
        given(weatherParserService.parseDailyForecast(any()))
                .willReturn(List.of(new DailyWeatherForecastDto(
                        forecastAt.toLocalDate(), SkyStatus.CLEAR,
                        20.0, 15.0, 25.0,
                        60.0, 0.0,
                        PrecipitationType.NONE, 0.0, 0.0,
                        2.0, 0.0)));

        JobExecution execution = jobLauncherTestUtils.launchJob(buildParams());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(weatherRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("다중 Location → 모두 처리, Job COMPLETED")
    void job_multipleLocations_processesAll() throws Exception {
        saveLocation(60, 127);
        saveLocation(61, 128);

        given(weatherApiService.fetchWeather(anyInt(), anyInt()))
                .willReturn(Mono.just(emptyResponse()));

        JobExecution execution = jobLauncherTestUtils.launchJob(buildParams());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    @DisplayName("일부 Location API 실패 → 실패 Location skip, 나머지 COMPLETED")
    void job_partialFailure_skipsFailedLocation() throws Exception {
        saveLocation(60, 127);
        saveLocation(61, 128);

        given(weatherApiService.fetchWeather(60, 127))
                .willReturn(Mono.error(new BusinessException(WeatherErrorCode.WEATHER_API_ERROR)));
        given(weatherApiService.fetchWeather(61, 128))
                .willReturn(Mono.just(emptyResponse()));

        JobExecution execution = jobLauncherTestUtils.launchJob(buildParams());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    private Location saveLocation(int gridX, int gridY) {
        return locationRepository.save(Location.builder()
                .latitude(37.5)
                .longitude(127.0)
                .gridX(gridX)
                .gridY(gridY)
                .locationNames("테스트 지역 " + gridX)
                .build());
    }

    private WeatherApiResponseDto emptyResponse() {
        return new WeatherApiResponseDto(
                new WeatherApiResponseDto.Response(
                        new WeatherApiResponseDto.Body(
                                new WeatherApiResponseDto.Items(List.of()))));
    }

    private JobParameters buildParams() {
        return new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
    }
}
