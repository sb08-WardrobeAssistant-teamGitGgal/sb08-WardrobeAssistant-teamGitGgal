package com.gitggal.clothesplz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                // 여기서 기본 URL이나 타임아웃 설정을 추가할 수 있음.
                .build();
    }
}