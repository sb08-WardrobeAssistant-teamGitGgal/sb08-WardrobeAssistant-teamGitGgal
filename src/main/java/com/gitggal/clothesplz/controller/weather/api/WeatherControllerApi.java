package com.gitggal.clothesplz.controller.weather.api;

import com.gitggal.clothesplz.dto.weather.WeatherAPILocationDto;
import com.gitggal.clothesplz.dto.weather.WeatherDto;
import com.gitggal.clothesplz.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "날씨 관리", description = "날씨 관련 API")
public interface WeatherControllerApi {

    @Operation(summary = "날씨 정보 조회", description = "날씨 정보 조회 API")
    @ApiResponse(
            responseCode = "200",
            description = "날씨 조회 성공",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = WeatherDto.class)))
    )
    @ApiResponse(
            responseCode = "400",
            description = "날씨 조회 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    List<WeatherDto> getWeather(
            @Parameter(description = "위도", required = true) double latitude,
            @Parameter(description = "경도", required = true) double longitude
    );

    @Operation(summary = "날씨 위치 정보 조회", description = "날씨 위치 정보 조회 API")
    @ApiResponse(
            responseCode = "200",
            description = "날씨 위치 정보 조회 성공",
            content = @Content(schema = @Schema(implementation = WeatherAPILocationDto.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "날씨 위치 정보 조회 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    WeatherAPILocationDto getWeatherLocation(
            @Parameter(description = "위도", required = true) double latitude,
            @Parameter(description = "경도", required = true) double longitude
    );
}
