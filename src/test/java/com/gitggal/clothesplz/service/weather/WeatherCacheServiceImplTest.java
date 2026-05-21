package com.gitggal.clothesplz.service.weather;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitggal.clothesplz.dto.weather.WeatherDto;
import com.gitggal.clothesplz.service.weather.impl.WeatherCacheServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayName("WeatherCacheService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class WeatherCacheServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @InjectMocks
    private WeatherCacheServiceImpl weatherCacheService;

    private static final int NX = 60;
    private static final int NY = 127;
    private static final String KEY = "weather:forecast:60:127";

    @Nested
    @DisplayName("getForecast()")
    class GetForecastTest {

        @BeforeEach
        void setUp() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
        }

        @Test
        @DisplayName("캐시 HIT → 데이터 반환")
        void getForecast_hit_returnsData() {
            List<WeatherDto> expected = List.of(mock(WeatherDto.class));
            given(valueOps.get(KEY)).willReturn(new Object());
            given(objectMapper.convertValue(any(), any(TypeReference.class))).willReturn(expected);

            Optional<List<WeatherDto>> result = weatherCacheService.getForecast(NX, NY);

            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(expected);
        }

        @Test
        @DisplayName("캐시 MISS (null) → Optional.empty()")
        void getForecast_miss_returnsEmpty() {
            given(valueOps.get(KEY)).willReturn(null);

            Optional<List<WeatherDto>> result = weatherCacheService.getForecast(NX, NY);

            assertThat(result).isEmpty();
            verifyNoInteractions(objectMapper);
        }

        @Test
        @DisplayName("Redis 예외 → Optional.empty() (fallback)")
        void getForecast_redisException_returnsEmpty() {
            given(valueOps.get(KEY)).willThrow(new RuntimeException("Redis down"));

            Optional<List<WeatherDto>> result = weatherCacheService.getForecast(NX, NY);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("saveForecast()")
    class SaveForecastTest {

        @BeforeEach
        void setUp() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
        }

        @Test
        @DisplayName("정상 저장 → set 호출 (TTL 3시간)")
        void saveForecast_success_callsSetWithTtl() {
            List<WeatherDto> data = List.of(mock(WeatherDto.class));

            weatherCacheService.saveForecast(NX, NY, data);

            verify(valueOps).set(eq(KEY), eq(data), eq(Duration.ofHours(3)));
        }

        @Test
        @DisplayName("Redis 예외 → 예외 전파 없음")
        void saveForecast_redisException_doesNotThrow() {
            doThrow(new RuntimeException("Redis down")).when(valueOps).set(any(), any(), any(Duration.class));

            assertThatCode(() -> weatherCacheService.saveForecast(NX, NY, List.of()))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("evictForecast()")
    class EvictForecastTest {

        @Test
        @DisplayName("정상 evict → delete 호출")
        void evictForecast_success_callsDelete() {
            weatherCacheService.evictForecast(NX, NY);

            verify(redisTemplate).delete(KEY);
        }

        @Test
        @DisplayName("Redis 예외 → 예외 전파 없음")
        void evictForecast_redisException_doesNotThrow() {
            doThrow(new RuntimeException("Redis down")).when(redisTemplate).delete(anyString());

            assertThatCode(() -> weatherCacheService.evictForecast(NX, NY))
                    .doesNotThrowAnyException();
        }
    }
}
