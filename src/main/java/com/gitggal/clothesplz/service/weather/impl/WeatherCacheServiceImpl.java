package com.gitggal.clothesplz.service.weather.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.dto.weather.WeatherDto;
import com.gitggal.clothesplz.service.weather.WeatherCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherCacheServiceImpl implements WeatherCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String FORECAST_KEY_PREFIX = "weather:forecast:";
    private static final Duration FORECAST_TTL = Duration.ofHours(3);

    @Override
    public Optional<List<WeatherDto>> getForecast(int nx, int ny) {
        String key = FORECAST_KEY_PREFIX + nx + ":" + ny;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached == null) {
                return Optional.empty();
            }
            log.debug("[Cache] HIT: key={}", key);
            return Optional.of(objectMapper.convertValue(cached, new TypeReference<>() {}));
        } catch (Exception e) {
            log.warn("[Cache] Redis 조회 실패, fallthrough: key={}, error={}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void saveForecast(int nx, int ny, List<WeatherDto> data) {
        String key = FORECAST_KEY_PREFIX + nx + ":" + ny;
        try {
            redisTemplate.opsForValue().set(key, data, FORECAST_TTL);
            log.debug("[Cache] SAVE: key={}, ttl={}h", key, FORECAST_TTL.toHours());
        } catch (Exception e) {
            log.warn("[Cache] Redis 저장 실패, 무시: key={}, error={}", key, e.getMessage());
        }
    }

    @Override
    public void evictForecast(int nx, int ny) {
        String key = FORECAST_KEY_PREFIX + nx + ":" + ny;
        try {
            redisTemplate.delete(key);
            log.debug("[Cache] EVICT: key={}", key);
        } catch (Exception e) {
            log.warn("[Cache] Redis evict 실패, 무시: key={}, error={}", key, e.getMessage());
        }
    }
}
