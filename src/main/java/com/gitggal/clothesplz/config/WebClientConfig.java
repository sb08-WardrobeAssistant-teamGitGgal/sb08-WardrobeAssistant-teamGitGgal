package com.gitggal.clothesplz.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        // Netty의 HttpClient를 사용하여 타임아웃 설정
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 연결 시도 타임아웃 (5초)
                .responseTimeout(Duration.ofSeconds(5))            // 전체 응답 대기 타임아웃 (5초)
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(5)) // 데이터 읽기 타임아웃 (5초)
                                .addHandlerLast(new WriteTimeoutHandler(5))); // 데이터 쓰기 타임아웃 (5초)

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}