package com.gitggal.clothesplz.component.batch.weather;

import com.gitggal.clothesplz.entity.weather.Location;
import com.gitggal.clothesplz.entity.weather.Weather;
import com.gitggal.clothesplz.repository.weather.WeatherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// 청크 단위 bulk 중복 체크 후 Weather 엔티티 일괄 저장
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherItemWriter implements ItemWriter<List<Weather>> {

    private final WeatherRepository weatherRepository;

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

        Set<String> existingKeys = weatherRepository
                .findByLocationInAndForecastAtBetween(locations, minForecastAt, maxForecastAt).stream()
                .map(w -> toKey(w.getLocation().getId(), w.getForecastAt()))
                .collect(Collectors.toSet());

        List<Weather> toSave = allWeathers.stream()
                .filter(w -> !existingKeys.contains(toKey(w.getLocation().getId(), w.getForecastAt())))
                .toList();

        weatherRepository.saveAll(toSave);
        log.debug("[Batch] 날씨 저장: {}건 (중복 제외: {}건)", toSave.size(), allWeathers.size() - toSave.size());
    }

    private String toKey(UUID locationId, OffsetDateTime forecastAt) {
        return locationId + "_" + forecastAt;
    }
}
