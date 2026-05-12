package com.gitggal.clothesplz.service.weather.impl;

import com.gitggal.clothesplz.dto.weather.WeatherApiResponseDto;
import com.gitggal.clothesplz.dto.weather.WeatherApiResponseDto.WeatherItem;
import com.gitggal.clothesplz.dto.weather.DailyWeatherForecastDto;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.SkyStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.time.format.DateTimeParseException;

@Slf4j
@Service
public class WeatherParserServiceImpl {

    /**
     * 기상청 응답 데이터를 분석하여 날짜별 요약 예보 리스트로 변환
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String REPRESENTATIVE_TIME = "1400";
    /** "1mm 미만" 같은 표현을 수치화할 때 쓰는 근사 오프셋 */
    private static final double UNDER_THRESHOLD_OFFSET = 0.1;

    public List<DailyWeatherForecastDto> parseDailyForecast(WeatherApiResponseDto response) {
        log.info("[Service] 기상청 응답 데이터 파싱 시작");

        if (response == null
                || response.response() == null
                || response.response().body() == null
                || response.response().body().items() == null
                || response.response().body().items().item() == null) {
            log.error("[Service] 기상청 응답 데이터가 비어있거나 올바르지 않습니다.");
            return Collections.emptyList();
        }

        List<WeatherItem> items = response.response().body().items().item();

        // 1. 날짜(fcstDate)별로 아이템들을 그룹화 (오늘, 내일, 모레...)
        Map<String, List<WeatherItem>> groupedByDate = items.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.fcstDate() != null && item.fcstDate().matches("\\d{8}")) // 날짜 형식 검증
                .collect(Collectors.groupingBy(WeatherItem::fcstDate));

        List<DailyWeatherForecastDto> dailyForecasts = new ArrayList<>();

        Double previousAvgTemp = null;
        Double previousHumidity = null;

        // 2. 날짜순으로 정렬하여 처리 (TreeSet 사용)
        for (String date : new TreeSet<>(groupedByDate.keySet())) {
            List<WeatherItem> dayItems = groupedByDate.get(date);

            Integer maxTemp = null;
            Integer minTemp = null;
            int sumTemp = 0;
            int tempCount = 0;
            String representativeSky = "1";
            List<Double> humidities = new ArrayList<>();
            List<Double> pops = new ArrayList<>();
            List<Double> winds = new ArrayList<>();
            double precipitationAmountSum = 0.0;
            boolean hasPrecipitationAmount = false;
            int precipitationPriority = -1;
            PrecipitationType precipitationType = PrecipitationType.NONE;

            for (WeatherItem item : dayItems) {
                if (item.category() == null) {
                    log.debug("[Service] category가 null인 항목 스킵: {}", item);
                    continue;
                }
                switch (item.category()) {
                    case "TMP" -> {
                        try{
                            int val = Integer.parseInt(item.fcstValue());
                            maxTemp = (maxTemp == null) ? val : Math.max(maxTemp, val);
                            minTemp = (minTemp == null) ? val : Math.min(minTemp, val);
                            sumTemp += val;
                            tempCount++;
                        } catch (NumberFormatException e) {
                            log.warn("[Service] TMP 값 파싱 실패: {}", item.fcstValue());
                        }
                    }
                    case "REH" -> parseDouble(item.fcstValue()).ifPresent(humidities::add);
                    case "POP" -> parseDouble(item.fcstValue()).ifPresent(pops::add);
                    case "WSD" -> parseDouble(item.fcstValue()).ifPresent(winds::add);
                    case "PCP" -> {
                        Optional<Double> pcp = parsePrecipitationAmount(item.fcstValue());
                        if (pcp.isPresent()) {
                            precipitationAmountSum += pcp.get();
                            hasPrecipitationAmount = true;
                        }
                    }
                    case "PTY" -> {
                        int nextPriority = parsePtyPriority(item.fcstValue(), item.fcstTime());
                        if (nextPriority > precipitationPriority) {
                            precipitationPriority = nextPriority;
                            precipitationType = convertPrecipitationType(item.fcstValue());
                        }
                    }
                    case "SKY" -> {
                        if (REPRESENTATIVE_TIME.equals(item.fcstTime()) && item.fcstValue() != null) {
                            representativeSky = item.fcstValue();
                        }
                    }
                    default -> {
                    }
                }
            }
            // 데이터가 없는 날은 리스트에 추가하지 않음 (NaN 방지)
            if (tempCount == 0) continue;

            // 수정 포인트: 정의한 DATE_FORMATTER를 사용하여 String -> LocalDate 변환
            LocalDate localDate;
            try{
                localDate = LocalDate.parse(date, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                log.warn("[Service] fcstDate 파싱 실패, 해당 날짜 스킵: {}", date);
                continue;
            }

            log.debug("[Service] 날짜별 가공 중: date={}, avgTemp={}", localDate, (double) sumTemp / tempCount);

            double avgTemp = tempCount > 0 ? Math.round((double) sumTemp / tempCount) : 0.0;
            double humidityCurrent = averageOrZero(humidities);
            double tempDiff = previousAvgTemp == null ? 0.0 : avgTemp - previousAvgTemp;
            double humidityDiff = previousHumidity == null ? 0.0 : humidityCurrent - previousHumidity;

            // DTO 생성 및 리스트 추가
            double minTempDouble = minTemp == null ? 0.0 : (double) minTemp;
            double maxTempDouble = maxTemp == null ? 0.0 : (double) maxTemp;

            dailyForecasts.add(
                    new DailyWeatherForecastDto(
                            localDate,
                            convertSkyStatus(representativeSky),
                            avgTemp,
                            minTempDouble,
                            maxTempDouble,
                            humidityCurrent,
                            humidityDiff,
                            precipitationType,
                            hasPrecipitationAmount ? precipitationAmountSum : 0.0,
                            maxOrZero(pops),
                            averageOrZero(winds),
                            tempDiff));

            previousAvgTemp = avgTemp;
            previousHumidity = humidityCurrent;
        }

        log.info("[Service] 기상청 응답 데이터 파싱 완료: 총 {}일치 데이터 생성", dailyForecasts.size());
        return dailyForecasts;
    }

    /**
     * 기상청 최신 SKY 명세 (2019.06.04 개정) 반영
     * 1: 맑음 (기상청이  구름 조금(2)와 1이 별 차이가 없어 2를 건너뜀)
     * 3: 구름많음
     * 4: 흐림
     */
    private SkyStatus convertSkyStatus(String skyCode) {
        return switch (skyCode) {
            case "1" -> SkyStatus.CLEAR;         // 맑음
            case "3" -> SkyStatus.MOSTLY_CLOUDY;  // 구름많음
            case "4" -> SkyStatus.CLOUDY;         // 흐림
            default -> SkyStatus.CLEAR;           // 예외 상황 발생 시 기본값 맑음
        };
    }

    private double averageOrZero(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double maxOrZero(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        return values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    private Optional<Double> parseDouble(String value) {
        try {
            return Optional.of(Double.parseDouble(value));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<Double> parsePrecipitationAmount(String value) {
        if (value == null || value.isBlank() || value.contains("강수없음")) {
            return Optional.empty();
        }
        String cleaned = value.replace("mm", "").replace(" ", "");
        if (cleaned.contains("미만")) {
            String numeric = cleaned.replace("미만", "");
            return parseDouble(numeric).map(v -> Math.max(0.0, v - UNDER_THRESHOLD_OFFSET));
        }
        if (cleaned.contains("이상")) {
            String numeric = cleaned.replace("이상", "");
            return parseDouble(numeric);
        }
        return parseDouble(cleaned);
    }

    private int parsePtyPriority(String pty, String fcstTime) {
        if (pty == null || "0".equals(pty)) {
            return -1;
        }
        int timePriority = REPRESENTATIVE_TIME.equals(fcstTime) ? 100 : 0;
        return timePriority + switch (pty) {
            case "4" -> 4;
            case "3" -> 3;
            case "2" -> 2;
            case "1" -> 1;
            default -> 0;
        };
    }

    private PrecipitationType convertPrecipitationType(String pty) {
        return switch (pty) {
            case "1" -> PrecipitationType.RAIN;
            case "2" -> PrecipitationType.RAIN_SNOW;
            case "3" -> PrecipitationType.SNOW;
            case "4" -> PrecipitationType.SHOWER;
            default -> PrecipitationType.NONE;
        };
    }
}