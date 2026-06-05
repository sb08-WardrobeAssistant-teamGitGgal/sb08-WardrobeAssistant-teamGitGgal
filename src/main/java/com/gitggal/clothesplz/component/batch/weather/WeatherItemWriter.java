package com.gitggal.clothesplz.component.batch.weather;

import com.gitggal.clothesplz.entity.weather.Location;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.repository.weather.WeatherRepository;
import com.gitggal.clothesplz.service.weather.WeatherAlertService;
import com.gitggal.clothesplz.service.weather.WeatherCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// 청크 단위 bulk 중복 체크 후 Weather 엔티티 일괄 저장
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherItemWriter implements ItemWriter<List<Weather>> {

    private final WeatherRepository weatherRepository;
    private final WeatherAlertService weatherAlertService;
    private final WeatherCacheService weatherCacheService;

    @Override
    public void write(Chunk<? extends List<Weather>> chunk) {
        List<Weather> allWeathers = chunk.getItems().stream()
                .flatMap(List::stream)
                .toList();

        if (allWeathers.isEmpty()) {
            return;
        }

        List<Location> locations = allWeathers.stream()
                .map(Weather::getLocation)
                .distinct()
                .toList();

        OffsetDateTime minForecastAt = allWeathers.stream()
                .map(Weather::getForecastAt)
                .min(Comparator.naturalOrder()).orElseThrow();
        OffsetDateTime maxForecastAt = allWeathers.stream()
                .map(Weather::getForecastAt)
                .max(Comparator.naturalOrder()).orElseThrow();

        Map<String, Weather> existingMap = weatherRepository
                .findByLocationInAndForecastAtBetween(locations, minForecastAt, maxForecastAt).stream()
                .collect(Collectors.toMap(w -> toKey(w.getLocation().getId(), w.getForecastAt()), w -> w));

        List<Weather> dedupedWeathers = new java.util.ArrayList<>(
                allWeathers.stream()
                        .collect(Collectors.toMap(
                                w -> toKey(w.getLocation().getId(), w.getForecastAt()),
                                Function.identity(),
                                (left, right) -> right,
                                LinkedHashMap::new
                        ))
                        .values()
        );

        List<Weather> toInsert = dedupedWeathers.stream()
                .filter(w -> !existingMap.containsKey(toKey(w.getLocation().getId(), w.getForecastAt())))
                .toList();

        List<Weather> toUpdate = dedupedWeathers.stream()
                .filter(w -> existingMap.containsKey(toKey(w.getLocation().getId(), w.getForecastAt())))
                .toList();

        weatherRepository.saveAll(toInsert);
        toUpdate.forEach(newData -> existingMap.get(toKey(newData.getLocation().getId(), newData.getForecastAt())).update(newData));
        log.debug("[Batch] 날씨 저장: 신규={}건, 업데이트={}건", toInsert.size(), toUpdate.size());

        locations.forEach(loc -> weatherCacheService.evictForecast(loc.getGridX(), loc.getGridY()));

        ZoneId seoul = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(seoul);
        Stream.concat(
                toInsert.stream(),
                toUpdate.stream().map(newData -> existingMap.get(toKey(newData.getLocation().getId(), newData.getForecastAt())))
        )
                .filter(w -> w.getForecastAt().atZoneSameInstant(seoul).toLocalDate().equals(today))
                .forEach(weatherAlertService::sendAlertsIfNeeded);
    }

    private String toKey(UUID locationId, OffsetDateTime forecastAt) {
        return locationId + "_" + forecastAt;
    }
}
