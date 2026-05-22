package com.gitggal.clothesplz.config;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfig {

    @Value("${spring.elasticsearch.username}")
    private String username;

    @Value("${spring.elasticsearch.password}")
    private String password;

    // DefaultRestClientBuilderCustomizer(Basic Auth)와 우리 Content-Type 픽스가
    // 각각 setHttpClientConfigCallback을 호출하면 마지막 호출이 앞 것을 덮어씀.
    // 두 설정을 하나의 콜백으로 통합해 충돌 방지.
    @Bean
    public RestClientBuilderCustomizer openSearchCompatibility() {
        return builder -> builder.setHttpClientConfigCallback(
            httpClientBuilder -> {
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();

                credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);

                // Amazon OpenSearch는 'compatible-with=8' Content-Type 미지원
                httpClientBuilder.addInterceptorLast(
                    (HttpRequestInterceptor) (request, context) -> {
                        request.removeHeaders("Content-Type");
                        request.addHeader("Content-Type", "application/json");
                        request.removeHeaders("Accept");
                        request.addHeader("Accept", "application/json, */*");
                    }
                );

                return httpClientBuilder;
            }
        );
    }
}
