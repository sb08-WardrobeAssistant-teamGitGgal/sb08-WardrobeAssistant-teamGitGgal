package com.gitggal.clothesplz.service.weather;

import com.gitggal.clothesplz.dto.weather.WeatherApiResponseDto;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.WeatherErrorCode;
import com.gitggal.clothesplz.service.weather.impl.WeatherApiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("WeatherApiServiceImpl 단위 테스트")
@ExtendWith(MockitoExtension.class)
class WeatherApiServiceImplTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private WeatherApiServiceImpl weatherApiService;

    @BeforeEach
    void setUp() {
        weatherApiService = new WeatherApiServiceImpl(webClient);
        ReflectionTestUtils.setField(weatherApiService, "apiUrl", "http://test.api/getVilageFcst");
        ReflectionTestUtils.setField(weatherApiService, "serviceKey", "test-key");

        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(any(URI.class));
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        doReturn(responseSpec).when(responseSpec).onStatus(any(Predicate.class), any(Function.class));
    }

    @Test
    @DisplayName("기상청 API 호출 성공 시 응답 DTO 반환")
    void fetchWeather_success_returnsDto() {
        WeatherApiResponseDto response = new WeatherApiResponseDto(null);
        when(responseSpec.bodyToMono(WeatherApiResponseDto.class)).thenReturn(Mono.just(response));

        WeatherApiResponseDto result = weatherApiService.fetchWeather(60, 127).block();

        assertThat(result).isEqualTo(response);
        verify(webClient, times(1)).get();
    }

    @Test
    @DisplayName("기상청 API HTTP 오류 시 BusinessException 전파")
    void fetchWeather_apiError_throwsBusinessException() {
        when(responseSpec.bodyToMono(WeatherApiResponseDto.class))
                .thenReturn(Mono.error(new BusinessException(WeatherErrorCode.WEATHER_API_ERROR)));

        assertThatThrownBy(() -> weatherApiService.fetchWeather(60, 127).block())
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("요청 URI에 nx, ny, numOfRows, dataType, base_date, base_time 파라미터 포함")
    void fetchWeather_buildsUriWithCorrectParams() {
        when(responseSpec.bodyToMono(WeatherApiResponseDto.class)).thenReturn(Mono.just(new WeatherApiResponseDto(null)));

        weatherApiService.fetchWeather(60, 127).block();

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(requestHeadersUriSpec).uri(uriCaptor.capture());
        String uri = uriCaptor.getValue().toString();

        assertThat(uri)
                .contains("nx=60")
                .contains("ny=127")
                .contains("numOfRows=1000")
                .contains("dataType=JSON")
                .containsPattern("base_date=\\d{8}")
                .containsPattern("base_time=\\d{4}");
    }
}
