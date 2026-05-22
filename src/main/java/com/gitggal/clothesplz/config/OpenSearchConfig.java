package com.gitggal.clothesplz.config;

import org.apache.http.HttpRequestInterceptor;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfig {

    // Amazon OpenSearch는 'compatible-with=8' Content-Type 미지원
    // 인터셉터로 application/json으로 교체
    @Bean
    public RestClientBuilderCustomizer openSearchCompatibility() {
        return builder -> builder.setHttpClientConfigCallback(
            httpClientBuilder -> httpClientBuilder.addInterceptorLast(
                (HttpRequestInterceptor) (request, context) -> {
                    request.removeHeaders("Content-Type");
                    request.addHeader("Content-Type", "application/json");
                    request.removeHeaders("Accept");
                    request.addHeader("Accept", "application/json, */*");
                }
            )
        );
    }
}
