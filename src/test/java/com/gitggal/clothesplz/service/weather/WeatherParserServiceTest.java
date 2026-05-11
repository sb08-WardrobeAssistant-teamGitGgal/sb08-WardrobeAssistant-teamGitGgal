package com.gitggal.clothesplz.service.weather;

import com.gitggal.clothesplz.dto.weather.DailyWeatherForecastDto;
import com.gitggal.clothesplz.dto.weather.WeatherApiResponseDto;
import com.gitggal.clothesplz.dto.weather.WeatherApiResponseDto.WeatherItem;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.SkyStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WeatherParserService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class WeatherParserServiceTest {

    @InjectMocks
    private WeatherParserService parserService;

    // ── 헬퍼 ─────────────────────────────────────────────────────────────

    private WeatherApiResponseDto wrap(List<WeatherItem> items) {
        return new WeatherApiResponseDto(
                new WeatherApiResponseDto.Response(
                        new WeatherApiResponseDto.Body(
                                new WeatherApiResponseDto.Items(items))));
    }

    private WeatherItem item(String category, String date, String time, String value) {
        return new WeatherItem(category, date, time, value);
    }

    // ── Null / 빈 응답 ────────────────────────────────────────────────────

    @Test
    @DisplayName("response가 null이면 빈 리스트를 반환한다")
    void parseDailyForecast_nullResponse_returnsEmpty() {
        assertThat(parserService.parseDailyForecast(null)).isEmpty();
    }

    @Test
    @DisplayName("items가 null이면 빈 리스트를 반환한다")
    void parseDailyForecast_nullItems_returnsEmpty() {
        WeatherApiResponseDto dto = new WeatherApiResponseDto(
                new WeatherApiResponseDto.Response(
                        new WeatherApiResponseDto.Body(
                                new WeatherApiResponseDto.Items(null))));
        assertThat(parserService.parseDailyForecast(dto)).isEmpty();
    }

    @Test
    @DisplayName("item 리스트가 비어있으면 빈 리스트를 반환한다")
    void parseDailyForecast_emptyItems_returnsEmpty() {
        assertThat(parserService.parseDailyForecast(wrap(Collections.emptyList()))).isEmpty();
    }

    // ── TMP (기온) ────────────────────────────────────────────────────────

    @Test
    @DisplayName("TMP가 없으면 해당 날짜는 결과에 포함되지 않는다")
    void parseDailyForecast_noTmp_dateSkipped() {
        List<WeatherItem> items = List.of(
                item("REH", "20260508", "0600", "50"),
                item("POP", "20260508", "0600", "10")
        );
        assertThat(parserService.parseDailyForecast(wrap(items))).isEmpty();
    }

    @Test
    @DisplayName("TMP에서 최고·최저·평균 기온을 올바르게 계산한다")
    void parseDailyForecast_tmp_calculatesMinMaxAvg() {
        List<WeatherItem> items = List.of(
                item("TMP", "20260508", "0300", "10"),
                item("TMP", "20260508", "0600", "12"),
                item("TMP", "20260508", "1400", "20")
        );
        List<DailyWeatherForecastDto> result = parserService.parseDailyForecast(wrap(items));

        assertThat(result).hasSize(1);
        DailyWeatherForecastDto day = result.get(0);
        assertThat(day.minTemp()).isEqualTo(10.0);
        assertThat(day.maxTemp()).isEqualTo(20.0);
        // avg = round((10+12+20)/3) = round(14.0) = 14.0
        assertThat(day.avgTemp()).isEqualTo(14.0);
    }

    @Test
    @DisplayName("TMP 값이 숫자가 아니면 해당 항목을 무시한다")
    void parseDailyForecast_invalidTmpValue_itemIgnored() {
        List<WeatherItem> items = List.of(
                item("TMP", "20260508", "0300", "N/A"),
                item("TMP", "20260508", "0600", "15")
        );
        List<DailyWeatherForecastDto> result = parserService.parseDailyForecast(wrap(items));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).avgTemp()).isEqualTo(15.0);
    }

    // ── SKY (하늘 상태) ───────────────────────────────────────────────────

    @Test
    @DisplayName("SKY는 1400 시각 값만 사용하며 1→CLEAR로 매핑한다")
    void parseDailyForecast_sky1at1400_returnsClear() {
        List<WeatherItem> items = List.of(
                item("TMP", "20260508", "1400", "18"),
                item("SKY", "20260508", "0600", "4"),
                item("SKY", "20260508", "1400", "1")
        );
        assertThat(parserService.parseDailyForecast(wrap(items)).get(0).skyStatus())
                .isEqualTo(SkyStatus.CLEAR);
    }

    @Test
    @DisplayName("SKY 코드 3은 MOSTLY_CLOUDY로 매핑한다")
    void parseDailyForecast_sky3at1400_returnsMostlyCloudy() {
        List<WeatherItem> items = List.of(
                item("TMP", "20260508", "1400", "18"),
                item("SKY", "20260508", "1400", "3")
        );
        assertThat(parserService.parseDailyForecast(wrap(items)).get(0).skyStatus())
                .isEqualTo(SkyStatus.MOSTLY_CLOUDY);
    }

    @Test
    @DisplayName("SKY 코드 4는 CLOUDY로 매핑한다")
    void parseDailyForecast_sky4at1400_returnsCloudy() {
        List<WeatherItem> items = List.of(
                item("TMP", "20260508", "1400", "18"),
                item("SKY", "20260508", "1400", "4")
        );
        assertThat(parserService.parseDailyForecast(wrap(items)).get(0).skyStatus())
                .isEqualTo(SkyStatus.CLOUDY);
    }

    @Test
    @DisplayName("SKY가 1400 시각에 없으면 기본값 CLEAR를 사용한다")
    void parseDailyForecast_skyMissingAt1400_defaultsClear() {
        List<WeatherItem> items = List.of(
                item("TMP", "20260508", "0600", "15"),
                item("SKY", "20260508", "0600", "4")
        );
        assertThat(parserService.parseDailyForecast(wrap(items)).get(0).skyStatus())
                .isEqualTo(SkyStatus.CLEAR);
    }

    // ── PTY (강수 형태) ───────────────────────────────────────────────────

    @Test
    @DisplayName("PTY 0은 NONE으로 처리한다")
    void parseDailyForecast_pty0_precipitationNone() {
        List<WeatherItem> items = List.of(
                item("TMP", "20260508", "0600", "15"),
                item("PTY", "20260508", "0600", "0")
        );
        assertThat(parserService.parseDailyForecast(wrap(items)).get(0).precipitationType())
                .isEqualTo(PrecipitationType.NONE);
    }

    @Test
    @DisplayName("PTY 1은 RAIN으로 매핑한다")
    void parseDailyForecast_pty1_rain() {
        List<WeatherItem> items = List.of(
                item("TMP", "20260508", "0600", "15"),
                item("PTY", "20260508", "0600", "1")
        );
        assertThat(parserService.parseDailyForecast(wrap(items)).get(0).precipitationType())
                .isEqualTo(PrecipitationType.RAIN);
    }

    @Test
    @DisplayName("PTY 1400 시각이 다른 시각보다 우선권이 높다")
    void parseDailyForecast_pty1400HasHigherPriority() {
        // 0600에 PTY=4(소나기), 1400에 PTY=1(비) → 1400 우선
        List<WeatherItem> items = List.of(
                item("TMP", "20260508", "0600", "15"),
                item("PTY", "20260508", "0600", "4"),
                item("PTY", "20260508", "1400", "1")
        );
        assertThat(parserService.parseDailyForecast(wrap(items)).get(0).precipitationType())
                .isEqualTo(PrecipitationType.RAIN);
    }

    @Test
    @DisplayName("같은 시각이면 PTY 코드 숫자가 높을수록 우선한다")
    void parseDailyForecast_ptyHigherCodeWins_sameTime() {
        // 코드 4(소나기) vs 코드 2(비·눈) → 4 우선
        List<WeatherItem> items = List.of(
                item("TMP", "20260508", "0600", "15"),
                item("PTY", "20260508", "0600", "2"),
                item("PTY", "20260508", "0900", "4")
        );
        assertThat(parserService.parseDailyForecast(wrap(items)).get(0).precipitationType())
                .isEqualTo(PrecipitationType.SHOWER);
    }

    // ── PCP (강수량) ──────────────────────────────────────────────────────

    @Test
    @DisplayName("강수없음은 precipitationAmount = 0.0을 반환한다")
    void parseDailyForecast_pcp_noRain_returnsZero() {
        List<WeatherItem> items = List.of(
                item("TMP", "20260508", "0600", "15"),
                item("PCP", "20260508", "0600", "강수없음")
        );
        assertThat(parserService.parseDailyForecast(wrap(items)).get(0).precipitationAmount())
                .isEqualTo(0.0);
    }

    @Test
    @DisplayName("'1mm 미만'은 0.9로 파싱한다")
    void parseDailyForecast_pcp_underThreshold_parsed() {
        List<WeatherItem> items = List.of(
                item("TMP", "20260508", "0600", "15"),
                item("PCP", "20260508", "0600", "1mm 미만")
        );
        assertThat(parserService.parseDailyForecast(wrap(items)).get(0).precipitationAmount())
                .isEqualTo(0.9);
    }

    @Test
    @DisplayName("'50mm 이상'은 50.0으로 파싱한다")
    void parseDailyForecast_pcp_overThreshold_parsed() {
        List<WeatherItem> items = List.of(
                item("TMP", "20260508", "0600", "15"),
                item("PCP", "20260508", "0600", "50mm 이상")
        );
        assertThat(parserService.parseDailyForecast(wrap(items)).get(0).precipitationAmount())
                .isEqualTo(50.0);
    }

    @Test
    @DisplayName("'5.5mm'은 5.5로 파싱한다")
    void parseDailyForecast_pcp_numericValue_parsed() {
        List<WeatherItem> items = List.of(
                item("TMP", "20260508", "0600", "15"),
                item("PCP", "20260508", "0600", "5.5mm")
        );
        assertThat(parserService.parseDailyForecast(wrap(items)).get(0).precipitationAmount())
                .isEqualTo(5.5);
    }

    // ── REH (습도) ────────────────────────────────────────────────────────

    @Test
    @DisplayName("REH 평균을 humidityCurrent로 반환한다")
    void parseDailyForecast_reh_returnsAverage() {
        List<WeatherItem> items = List.of(
                item("TMP", "20260508", "0600", "15"),
                item("REH", "20260508", "0600", "40"),
                item("REH", "20260508", "1200", "60")
        );
        assertThat(parserService.parseDailyForecast(wrap(items)).get(0).humidityCurrent())
                .isEqualTo(50.0);
    }

    // ── POP (강수 확률) ───────────────────────────────────────────────────

    @Test
    @DisplayName("POP 최댓값을 precipitationProbability로 반환한다")
    void parseDailyForecast_pop_returnsMax() {
        List<WeatherItem> items = List.of(
                item("TMP", "20260508", "0600", "15"),
                item("POP", "20260508", "0600", "20"),
                item("POP", "20260508", "1200", "80")
        );
        assertThat(parserService.parseDailyForecast(wrap(items)).get(0).precipitationProbability())
                .isEqualTo(80.0);
    }

    // ── WSD (풍속) ────────────────────────────────────────────────────────

    @Test
    @DisplayName("WSD 평균을 windSpeed로 반환한다")
    void parseDailyForecast_wsd_returnsAverage() {
        List<WeatherItem> items = List.of(
                item("TMP", "20260508", "0600", "15"),
                item("WSD", "20260508", "0600", "2.0"),
                item("WSD", "20260508", "1200", "4.0")
        );
        assertThat(parserService.parseDailyForecast(wrap(items)).get(0).windSpeed())
                .isEqualTo(3.0);
    }

    // ── 날짜 필터링 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("fcstDate 형식이 8자리 숫자가 아니면 해당 항목을 필터링한다")
    void parseDailyForecast_invalidDateFormat_filtered() {
        List<WeatherItem> items = List.of(
                item("TMP", "2026-05-08", "0600", "15")
        );
        assertThat(parserService.parseDailyForecast(wrap(items))).isEmpty();
    }

    @Test
    @DisplayName("category가 null인 항목은 스킵한다")
    void parseDailyForecast_nullCategory_itemSkipped() {
        List<WeatherItem> items = List.of(
                item(null, "20260508", "0600", "99"),
                item("TMP", "20260508", "0600", "15")
        );
        assertThat(parserService.parseDailyForecast(wrap(items))).hasSize(1);
    }

    // ── 다중 날짜 / 기온 차 계산 ──────────────────────────────────────────

    @Test
    @DisplayName("여러 날짜를 날짜 순으로 정렬하여 반환한다")
    void parseDailyForecast_multipleDates_sortedAscending() {
        List<WeatherItem> items = List.of(
                item("TMP", "20260510", "0600", "20"),
                item("TMP", "20260508", "0600", "10"),
                item("TMP", "20260509", "0600", "15")
        );
        List<DailyWeatherForecastDto> result = parserService.parseDailyForecast(wrap(items));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).avgTemp()).isEqualTo(10.0);
        assertThat(result.get(1).avgTemp()).isEqualTo(15.0);
        assertThat(result.get(2).avgTemp()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("첫째 날 temperatureComparedToDayBefore는 0.0이고 이후 날은 이전 날과의 차이다")
    void parseDailyForecast_tempDiff_calculatedCorrectly() {
        List<WeatherItem> items = List.of(
                item("TMP", "20260508", "0600", "10"),
                item("TMP", "20260509", "0600", "16")
        );
        List<DailyWeatherForecastDto> result = parserService.parseDailyForecast(wrap(items));

        assertThat(result.get(0).temperatureComparedToDayBefore()).isEqualTo(0.0);
        assertThat(result.get(1).temperatureComparedToDayBefore()).isEqualTo(6.0);
    }
}
