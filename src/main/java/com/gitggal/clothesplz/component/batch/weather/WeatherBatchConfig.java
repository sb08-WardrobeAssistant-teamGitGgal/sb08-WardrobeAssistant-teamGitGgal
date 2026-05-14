package com.gitggal.clothesplz.component.batch.weather;

import com.gitggal.clothesplz.entity.weather.Location;
import com.gitggal.clothesplz.entity.weather.Weather;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

// 기상청 단기예보 배치 Job/Step/Reader 빈 정의
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class WeatherBatchConfig {

    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job weatherFetchJob(JobRepository jobRepository, Step weatherFetchStep) {
        return new JobBuilder("weatherFetchJob", jobRepository)
                .start(weatherFetchStep)
                .build();
    }

    @Bean
    public Step weatherFetchStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            WeatherFetchProcessor processor,
            WeatherItemWriter writer) {
        return new StepBuilder("weatherFetchStep", jobRepository)
                .<Location, List<Weather>>chunk(10, transactionManager)
                .reader(locationItemReader())
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(10)
                .build();
    }

    @Bean
    public JpaCursorItemReader<Location> locationItemReader() {
        return new JpaCursorItemReaderBuilder<Location>()
                .name("locationItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT l FROM Location l")
                .build();
    }
}
