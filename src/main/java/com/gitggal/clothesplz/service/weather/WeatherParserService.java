package com.gitggal.clothesplz.service.weather;

import com.gitggal.clothesplz.dto.weather.WeatherApiResponseDto;
import com.gitggal.clothesplz.dto.weather.WeatherApiResponseDto.WeatherItem;
import com.gitggal.clothesplz.dto.weather.DailyWeatherForecastDto;
import com.gitggal.clothesplz.entity.weather.SkyStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate; // 추가
import java.time.format.DateTimeFormatter; // 추가
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WeatherParserService {

    /**
     * 기상청 응답 데이터를 분석하여 날짜별 요약 예보 리스트로 변환
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public List<DailyWeatherForecastDto> parseDailyForecast(WeatherApiResponseDto response) {
        log.info("[Service] 기상청 응답 데이터 파싱 시작");

        if (response == null ||
                response.getResponse() == null ||
                response.getResponse().getBody() == null ||
                response.getResponse().getBody().getItems() == null) {
            log.error("[Service] 기상청 응답 데이터가 비어있거나 올바르지 않습니다.");
            return Collections.emptyList();
        }

        List<WeatherItem> items = response.getResponse().getBody().getItems().getItem();

        // 1. 날짜(fcstDate)별로 아이템들을 그룹화 (오늘, 내일, 모레...)
        Map<String, List<WeatherItem>> groupedByDate = items.stream()
                .collect(Collectors.groupingBy(WeatherItem::getFcstDate));

        List<DailyWeatherForecastDto> dailyForecasts = new ArrayList<>();

        // 2. 날짜순으로 정렬하여 처리 (TreeSet 사용)
        for (String date : new TreeSet<>(groupedByDate.keySet())) {
            List<WeatherItem> dayItems = groupedByDate.get(date);

            Integer maxTemp = null;
            Integer minTemp = null;
            int sumTemp = 0;
            int tempCount = 0;
            String representativeSky = "1";

            for (WeatherItem item : dayItems) {
                if ("TMP".equals(item.getCategory())) {
                    int val = Integer.parseInt(item.getFcstValue());
                    maxTemp = (maxTemp == null) ? val : Math.max(maxTemp, val);
                    minTemp = (minTemp == null) ? val : Math.min(minTemp, val);
                    sumTemp += val;
                    tempCount++;
                }
                // 오후 2시 대표 기상
                if ("SKY".equals(item.getCategory()) && "1400".equals(item.getFcstTime())) {
                    representativeSky = item.getFcstValue();
                }
            }
            // 데이터가 없는 날은 리스트에 추가하지 않음 (NaN 방지)
            if (tempCount == 0) continue;

            // 수정 포인트: 정의한 DATE_FORMATTER를 사용하여 String -> LocalDate 변환
            LocalDate localDate = LocalDate.parse(date, DATE_FORMATTER);

            log.debug("[Service] 날짜별 가공 중: date={}, avgTemp={}", localDate, (double) sumTemp / tempCount);

            // DTO 생성 및 리스트 추가
            dailyForecasts.add(DailyWeatherForecastDto.builder()
                    .date(localDate)
                    .avgTemp(tempCount > 0 ? (double) Math.round((double) sumTemp / tempCount) : 0.0)
                    .maxTemp((double) maxTemp)
                    .minTemp((double) minTemp)
                    .skyStatus(convertSkyStatus(representativeSky))
                    .build());
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
}