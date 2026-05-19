package com.gitggal.clothesplz.service.weather;

import reactor.core.publisher.Mono;

import java.util.List;

public interface KakaoLocalApiService {

    Mono<List<String>> getLocationNames(double latitude, double longitude);
}
