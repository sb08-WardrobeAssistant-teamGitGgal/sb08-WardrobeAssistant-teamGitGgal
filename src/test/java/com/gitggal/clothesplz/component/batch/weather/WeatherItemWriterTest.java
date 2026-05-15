package com.gitggal.clothesplz.component.batch.weather;

import com.gitggal.clothesplz.entity.weather.*;
import com.gitggal.clothesplz.repository.weather.WeatherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("WeatherItemWriter 단위 테스트")
@ExtendWith(MockitoExtension.class)
class WeatherItemWriterTest {

    @Mock
    private WeatherRepository weatherRepository;

    @InjectMocks
    private WeatherItemWriter writer;

    private Location location;
    private UUID locationId;

    @BeforeEach
    void setUp() {
        locationId = UUID.randomUUID();
        location = mock(Location.class);
        given(location.getId()).willReturn(locationId);
    }

    @Nested
    @DisplayName("write()")
    class WriteTest {

        @Test
        @DisplayName("모두 신규 데이터 → 전체 saveAll")
        void write_allNew_savesAll() throws Exception {
            OffsetDateTime forecastAt1 = OffsetDateTime.now();
            OffsetDateTime forecastAt2 = OffsetDateTime.now().plusDays(1);

            Weather w1 = makeWeather(forecastAt1);
            Weather w2 = makeWeather(forecastAt2);
            Chunk<List<Weather>> chunk = new Chunk<>(List.of(List.of(w1, w2)));

            given(weatherRepository.findByLocationInAndForecastAtBetween(anyList(), any(), any()))
                    .willReturn(List.of());

            writer.write(chunk);

            ArgumentCaptor<List<Weather>> captor = ArgumentCaptor.forClass(List.class);
            verify(weatherRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(2);
        }

        @Test
        @DisplayName("모두 중복 데이터 → saveAll 빈 리스트")
        void write_allDuplicate_savesNothing() throws Exception {
            OffsetDateTime forecastAt = OffsetDateTime.now();

            Weather w1 = makeWeather(forecastAt);
            Weather existing = makeWeather(forecastAt);
            Chunk<List<Weather>> chunk = new Chunk<>(List.of(List.of(w1)));

            given(weatherRepository.findByLocationInAndForecastAtBetween(anyList(), any(), any()))
                    .willReturn(List.of(existing));

            writer.write(chunk);

            ArgumentCaptor<List<Weather>> captor = ArgumentCaptor.forClass(List.class);
            verify(weatherRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).isEmpty();
        }

        @Test
        @DisplayName("신규 + 중복 혼재 → 신규만 saveAll")
        void write_mixed_savesOnlyNew() throws Exception {
            OffsetDateTime existingAt = OffsetDateTime.now();
            OffsetDateTime newAt = OffsetDateTime.now().plusDays(1);

            Weather existing = makeWeather(existingAt);
            Weather newWeather = makeWeather(newAt);
            Chunk<List<Weather>> chunk = new Chunk<>(List.of(List.of(existing, newWeather)));

            given(weatherRepository.findByLocationInAndForecastAtBetween(anyList(), any(), any()))
                    .willReturn(List.of(existing));

            writer.write(chunk);

            ArgumentCaptor<List<Weather>> captor = ArgumentCaptor.forClass(List.class);
            verify(weatherRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().get(0).getForecastAt()).isEqualTo(newAt);
        }

        @Test
        @DisplayName("여러 chunk item → flatten 후 bulk 처리")
        void write_multipleChunkItems_flattensAndBulkProcesses() throws Exception {
            OffsetDateTime at1 = OffsetDateTime.now();
            OffsetDateTime at2 = OffsetDateTime.now().plusDays(1);

            Weather w1 = makeWeather(at1);
            Weather w2 = makeWeather(at2);
            // 두 location의 Weather가 별도 List로 chunk에 담긴 상황
            Chunk<List<Weather>> chunk = new Chunk<>(List.of(List.of(w1), List.of(w2)));

            given(weatherRepository.findByLocationInAndForecastAtBetween(anyList(), any(), any()))
                    .willReturn(List.of());

            writer.write(chunk);

            ArgumentCaptor<List<Weather>> captor = ArgumentCaptor.forClass(List.class);
            verify(weatherRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(2);
        }
    }

    private Weather makeWeather(OffsetDateTime forecastAt) {
        Weather weather = mock(Weather.class);
        given(weather.getLocation()).willReturn(location);
        given(weather.getForecastAt()).willReturn(forecastAt);
        return weather;
    }
}
