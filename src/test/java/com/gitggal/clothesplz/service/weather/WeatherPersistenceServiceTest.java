package com.gitggal.clothesplz.service.weather;

import com.gitggal.clothesplz.dto.weather.DailyWeatherForecastDto;
import com.gitggal.clothesplz.entity.weather.Location;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.SkyStatus;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.entity.weather.WindPhrase;
import com.gitggal.clothesplz.repository.weather.LocationRepository;
import com.gitggal.clothesplz.repository.weather.WeatherRepository;
import com.gitggal.clothesplz.service.weather.impl.WeatherPersistenceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherPersistenceServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private WeatherRepository weatherRepository;

    @InjectMocks
    private WeatherPersistenceService weatherPersistenceService;

    // ===== findOrCreateLocation =====

    @Test
    @DisplayName("격자 좌표로 기존 Location이 있으면 저장 없이 반환한다")
    void findOrCreateLocation_exists_returnsExisting() {
        Location existing = mock(Location.class);
        when(locationRepository.findByGridXAndGridY(60, 127)).thenReturn(Optional.of(existing));

        Location result = weatherPersistenceService.findOrCreateLocation(37.5665, 126.9780, 60, 127, "서울,중구");

        assertThat(result).isSameAs(existing);
        verify(locationRepository, never()).save(any());
    }

    @Test
    @DisplayName("격자 좌표로 Location이 없으면 새로 저장해 반환한다")
    void findOrCreateLocation_notExists_savesAndReturns() {
        Location saved = mock(Location.class);
        when(locationRepository.findByGridXAndGridY(60, 127)).thenReturn(Optional.empty());
        when(locationRepository.save(any())).thenReturn(saved);

        Location result = weatherPersistenceService.findOrCreateLocation(37.5665, 126.9780, 60, 127, "서울,중구");

        assertThat(result).isSameAs(saved);
        verify(locationRepository).save(any(Location.class));
    }

    // ===== findLocationOrThrow =====

    @Test
    @DisplayName("격자 좌표로 Location이 있으면 반환한다")
    void findLocationOrThrow_exists_returns() {
        Location existing = mock(Location.class);
        when(locationRepository.findByGridXAndGridY(60, 127)).thenReturn(Optional.of(existing));

        Location result = weatherPersistenceService.findLocationOrThrow(60, 127);

        assertThat(result).isSameAs(existing);
    }

    @Test
    @DisplayName("격자 좌표로 Location이 없으면 NoSuchElementException을 던진다")
    void findLocationOrThrow_notExists_throws() {
        when(locationRepository.findByGridXAndGridY(60, 127)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> weatherPersistenceService.findLocationOrThrow(60, 127))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ===== findOrCreateWeather =====

    @Test
    @DisplayName("해당 위치·날짜의 Weather가 이미 있으면 저장 없이 반환한다")
    void findOrCreateWeather_exists_returnsExisting() {
        Location location = mock(Location.class);
        Weather existing = mock(Weather.class);
        DailyWeatherForecastDto dto = makeDto(LocalDate.of(2026, 5, 18), PrecipitationType.NONE, 3.0);
        OffsetDateTime forecastAt = LocalDate.of(2026, 5, 18).atStartOfDay().atZone(KST).toOffsetDateTime();

        when(weatherRepository.findFirstByLocationAndForecastAt(location, forecastAt)).thenReturn(Optional.of(existing));

        Weather result = weatherPersistenceService.findOrCreateWeather(location, dto);

        assertThat(result).isSameAs(existing);
        verify(weatherRepository, never()).save(any());
    }

    @Test
    @DisplayName("Weather가 없으면 새로 저장한다")
    void findOrCreateWeather_notExists_savesAndReturns() {
        Location location = mock(Location.class);
        DailyWeatherForecastDto dto = makeDto(LocalDate.of(2026, 5, 18), PrecipitationType.RAIN, 3.0);
        OffsetDateTime forecastAt = LocalDate.of(2026, 5, 18).atStartOfDay().atZone(KST).toOffsetDateTime();

        when(weatherRepository.findFirstByLocationAndForecastAt(location, forecastAt)).thenReturn(Optional.empty());
        Weather saved = mock(Weather.class);
        when(weatherRepository.save(any())).thenReturn(saved);

        Weather result = weatherPersistenceService.findOrCreateWeather(location, dto);

        assertThat(result).isSameAs(saved);
        verify(weatherRepository).save(any(Weather.class));
    }

    @Test
    @DisplayName("precipitationType이 null이면 NONE으로 저장한다")
    void findOrCreateWeather_nullPrecipitationType_savesAsNone() {
        Location location = mock(Location.class);
        DailyWeatherForecastDto dto = makeDto(LocalDate.of(2026, 5, 18), null, 3.0);
        OffsetDateTime forecastAt = LocalDate.of(2026, 5, 18).atStartOfDay().atZone(KST).toOffsetDateTime();

        when(weatherRepository.findFirstByLocationAndForecastAt(location, forecastAt)).thenReturn(Optional.empty());
        ArgumentCaptor<Weather> captor = ArgumentCaptor.forClass(Weather.class);
        when(weatherRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        weatherPersistenceService.findOrCreateWeather(location, dto);

        assertThat(captor.getValue().getPrecipitationType()).isEqualTo(PrecipitationType.NONE);
    }

    @Test
    @DisplayName("수치형 필드가 모두 null이면 0.0으로 저장한다")
    void findOrCreateWeather_nullNumerics_savesAsZero() {
        Location location = mock(Location.class);
        DailyWeatherForecastDto dto = new DailyWeatherForecastDto(
                LocalDate.of(2026, 5, 18), SkyStatus.CLEAR,
                null, null, null, null, null, null,
                null, null, null, null);
        OffsetDateTime forecastAt = LocalDate.of(2026, 5, 18).atStartOfDay().atZone(KST).toOffsetDateTime();

        when(weatherRepository.findFirstByLocationAndForecastAt(location, forecastAt)).thenReturn(Optional.empty());
        ArgumentCaptor<Weather> captor = ArgumentCaptor.forClass(Weather.class);
        when(weatherRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        weatherPersistenceService.findOrCreateWeather(location, dto);

        Weather result = captor.getValue();
        assertThat(result.getPrecipitationAmount()).isZero();
        assertThat(result.getPrecipitationProbability()).isZero();
        assertThat(result.getHumidity()).isZero();
        assertThat(result.getHumidityDiff()).isZero();
        assertThat(result.getTemperatureCurrent()).isZero();
        assertThat(result.getTemperatureDiff()).isZero();
        assertThat(result.getTemperatureMin()).isZero();
        assertThat(result.getTemperatureMax()).isZero();
        assertThat(result.getWindSpeed()).isZero();
        assertThat(result.getWindPhrase()).isEqualTo(WindPhrase.WEAK);
    }

    @Test
    @DisplayName("풍속이 null이면 windPhrase는 WEAK이다")
    void findOrCreateWeather_nullWindSpeed_savesWeak() {
        assertWindPhrase(null, WindPhrase.WEAK);
    }

    @Test
    @DisplayName("풍속이 4.0 미만이면 windPhrase는 WEAK이다")
    void findOrCreateWeather_windBelowModerate_savesWeak() {
        assertWindPhrase(3.9, WindPhrase.WEAK);
    }

    @Test
    @DisplayName("풍속이 4.0 이상 9.0 미만이면 windPhrase는 MODERATE이다")
    void findOrCreateWeather_windModerate_savesModerate() {
        assertWindPhrase(4.0, WindPhrase.MODERATE);
    }

    @Test
    @DisplayName("풍속이 9.0 이상이면 windPhrase는 STRONG이다")
    void findOrCreateWeather_windStrong_savesStrong() {
        assertWindPhrase(9.0, WindPhrase.STRONG);
    }

    // ===== findWeatherOrThrow =====

    @Test
    @DisplayName("위치와 예보 시각으로 Weather가 있으면 반환한다")
    void findWeatherOrThrow_exists_returns() {
        Location location = mock(Location.class);
        Weather existing = mock(Weather.class);
        OffsetDateTime forecastAt = LocalDate.of(2026, 5, 18).atStartOfDay().atZone(KST).toOffsetDateTime();

        when(weatherRepository.findFirstByLocationAndForecastAt(location, forecastAt)).thenReturn(Optional.of(existing));

        Weather result = weatherPersistenceService.findWeatherOrThrow(location, forecastAt);

        assertThat(result).isSameAs(existing);
    }

    @Test
    @DisplayName("위치와 예보 시각으로 Weather가 없으면 NoSuchElementException을 던진다")
    void findWeatherOrThrow_notExists_throws() {
        Location location = mock(Location.class);
        OffsetDateTime forecastAt = LocalDate.of(2026, 5, 18).atStartOfDay().atZone(KST).toOffsetDateTime();

        when(weatherRepository.findFirstByLocationAndForecastAt(location, forecastAt)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> weatherPersistenceService.findWeatherOrThrow(location, forecastAt))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ===== helpers =====

    private DailyWeatherForecastDto makeDto(LocalDate date, PrecipitationType precipitationType, Double windSpeed) {
        return new DailyWeatherForecastDto(
                date, SkyStatus.CLEAR,
                16.0, 11.0, 20.0, 42.0, -2.0, precipitationType,
                0.5, 30.0, windSpeed, 1.0);
    }

    private void assertWindPhrase(Double windSpeed, WindPhrase expected) {
        Location location = mock(Location.class);
        DailyWeatherForecastDto dto = makeDto(LocalDate.of(2026, 5, 18), PrecipitationType.NONE, windSpeed);
        OffsetDateTime forecastAt = LocalDate.of(2026, 5, 18).atStartOfDay().atZone(KST).toOffsetDateTime();

        when(weatherRepository.findFirstByLocationAndForecastAt(location, forecastAt)).thenReturn(Optional.empty());
        ArgumentCaptor<Weather> captor = ArgumentCaptor.forClass(Weather.class);
        when(weatherRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        weatherPersistenceService.findOrCreateWeather(location, dto);

        assertThat(captor.getValue().getWindPhrase()).isEqualTo(expected);
    }
}
