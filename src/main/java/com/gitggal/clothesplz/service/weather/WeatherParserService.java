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

        if (response == null || response.getResponse().getBody() == null) {
            log.error("[Service] 기상청 응답 데이터가 비어있어 파싱 중단.");
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

            int maxTemp = -99;
            int minTemp = 99;
            int sumTemp = 0;
            int tempCount = 0;
            String representativeSky = "1";

            for (WeatherItem item : dayItems) {
                // 기온 데이터(TMP) 처리
                if (item.getCategory().equals("TMP")) {
                    int val = Integer.parseInt(item.getFcstValue());
                    maxTemp = Math.max(maxTemp, val);
                    minTemp = Math.min(minTemp, val);
                    sumTemp += val;
                    tempCount++;
                }

                // 오후 2시(1400)의 하늘 상태를 그 날의 낮 기상 대표값으로 사용
                if (item.getCategory().equals("SKY") && item.getFcstTime().equals("1400")) {
                    representativeSky = item.getFcstValue();
                }
            }

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
     * 기상청 SKY 코드를 요청하신 3가지 상태로 변환
     * 기상청 기준: 1(맑음), 2(구름많음), 3(흐림)
     */
    private SkyStatus convertSkyStatus(String skyCode) {
        return switch (skyCode) {
            case "1" -> SkyStatus.CLEAR;
            case "2" -> SkyStatus.MOSTLY_CLOUDY;
            case "3" -> SkyStatus.CLOUDY;
            default -> SkyStatus.CLEAR;
        };
    }
}