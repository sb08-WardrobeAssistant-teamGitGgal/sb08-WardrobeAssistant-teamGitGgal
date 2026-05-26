package com.gitggal.clothesplz.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
    info = @Info(title = "옷장을 부탁해 API 문서",
        description = "SB 8기 1팀 Git깔나는 조의 옷장을 부탁해 프로젝트 API 문서입니다.",
        version = "1.0")
)
@Configuration
public class SwaggerConfig {

}
