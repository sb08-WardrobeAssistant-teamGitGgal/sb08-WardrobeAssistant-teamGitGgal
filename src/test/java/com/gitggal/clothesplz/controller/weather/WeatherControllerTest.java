package com.gitggal.clothesplz.controller.weather;

import com.gitggal.clothesplz.dto.weather.HumidityDto;
import com.gitggal.clothesplz.dto.weather.PrecipitationDto;
import com.gitggal.clothesplz.dto.weather.TemperatureDto;
import com.gitggal.clothesplz.dto.weather.WeatherAPILocationDto;
import com.gitggal.clothesplz.dto.weather.WeatherDto;
import com.gitggal.clothesplz.dto.weather.WindSpeedDto;
import com.gitggal.clothesplz.entity.weather.PrecipitationType;
import com.gitggal.clothesplz.entity.weather.SkyStatus;
import com.gitggal.clothesplz.entity.weather.WindPhrase;
import com.gitggal.clothesplz.config.TestSecurityConfig;
import com.gitggal.clothesplz.exception.GlobalExceptionHandler;
import com.gitggal.clothesplz.exception.code.WeatherErrorCode;
import com.gitggal.clothesplz.security.jwt.JwtAuthenticationFilter;
import com.gitggal.clothesplz.service.weather.WeatherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("날씨 컨트롤러 테스트")
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@WebMvcTest(
        controllers = WeatherController.class,
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = JwtAuthenticationFilter.class
                )
        }
)
class WeatherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeatherService weatherService;

    @Test
    @DisplayName("날씨 조회 시 200 OK와 응답 데이터를 반환한다")
    void getWeathers_validRequest_returns200() throws Exception {
        // given
        WeatherDto response = new WeatherDto(
                UUID.randomUUID(),
                LocalDateTime.of(2026, 5, 8, 8, 18, 45),
                LocalDateTime.of(2026, 5, 8, 8, 18, 45),
                new WeatherAPILocationDto(37.5665, 126.9780, 60, 127, List.of("서울특별시", "중구")),
                SkyStatus.CLEAR,
                new PrecipitationDto(PrecipitationType.NONE, 0.1, 0.1),
                new HumidityDto(42.0, 0.1),
                new TemperatureDto(16.0, 0.1, 11.0, 20.0),
                new WindSpeedDto(0.1, WindPhrase.WEAK)
        );
        given(weatherService.getWeatherForecast(anyDouble(), anyDouble()))
                .willReturn(Mono.just(List.of(response)));

        // when & then
        MvcResult mvcResult = mockMvc.perform(get("/api/weathers")
                        .param("latitude", "37.5665")
                        .param("longitude", "126.9780"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skyStatus").value("CLEAR"))
                .andExpect(jsonPath("$[0].location.x").value(60))
                .andExpect(jsonPath("$[0].location.y").value(127))
                .andExpect(jsonPath("$[0].precipitation.type").value("NONE"))
                .andExpect(jsonPath("$[0].humidity.current").value(42.0))
                .andExpect(jsonPath("$[0].temperature.current").value(16.0))
                .andExpect(jsonPath("$[0].windSpeed.asWord").value("WEAK"));
    }

    @Test
    @DisplayName("날씨 조회 중 서비스 오류가 발생하면 500을 반환한다")
    void getWeathers_serviceError_returns500() throws Exception {
        // given
        given(weatherService.getWeatherForecast(anyDouble(), anyDouble()))
                .willReturn(Mono.error(new RuntimeException("downstream error")));

        // when & then
        MvcResult mvcResult = mockMvc.perform(get("/api/weathers")
                        .param("latitude", "37.5665")
                        .param("longitude", "126.9780"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.exceptionName").value("WEATHER_API_ERROR"))
                .andExpect(jsonPath("$.message").value(WeatherErrorCode.WEATHER_API_ERROR.getMessage()));
    }

    @Test
    @DisplayName("latitude 파라미터가 없으면 400을 반환한다")
    void getWeathers_missingLatitude_returns400() throws Exception {
        mockMvc.perform(get("/api/weathers")
                        .param("longitude", "126.9780"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionName").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("longitude 타입이 잘못되면 400을 반환한다")
    void getWeathers_invalidLongitudeType_returns400() throws Exception {
        mockMvc.perform(get("/api/weathers")
                        .param("latitude", "37.5665")
                        .param("longitude", "bad"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionName").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("날씨 위치 조회 시 200 OK와 위치 데이터를 반환한다")
    void getWeatherLocation_validRequest_returns200() throws Exception {
        // given
        WeatherAPILocationDto response = new WeatherAPILocationDto(
                37.5665, 126.9780, 60, 127, List.of("서울특별시", "중구"));
        given(weatherService.getWeatherLocation(anyDouble(), anyDouble())).willReturn(Mono.just(response));

        // when & then
        MvcResult mvcResult = mockMvc.perform(get("/api/weathers/location")
                        .param("latitude", "37.5665")
                        .param("longitude", "126.9780"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.x").value(60))
                .andExpect(jsonPath("$.y").value(127))
                .andExpect(jsonPath("$.locationNames[0]").value("서울특별시"));
    }

    @Test
    @DisplayName("날씨 위치 조회 중 서비스 오류가 발생하면 500을 반환한다")
    void getWeatherLocation_serviceError_returns500() throws Exception {
        // given
        given(weatherService.getWeatherLocation(anyDouble(), anyDouble()))
                .willReturn(Mono.error(new RuntimeException("location error")));

        // when & then
        MvcResult mvcResult = mockMvc.perform(get("/api/weathers/location")
                        .param("latitude", "37.5665")
                        .param("longitude", "126.9780"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.exceptionName").value("WEATHER_API_ERROR"))
                .andExpect(jsonPath("$.message").value(WeatherErrorCode.WEATHER_API_ERROR.getMessage()));
    }

    @Test
    @DisplayName("날씨 위치 조회 파라미터 누락 시 400을 반환한다")
    void getWeatherLocation_missingLongitude_returns400() throws Exception {
        mockMvc.perform(get("/api/weathers/location")
                        .param("latitude", "37.5665"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.exceptionName").value("INVALID_INPUT"));
    }
}

