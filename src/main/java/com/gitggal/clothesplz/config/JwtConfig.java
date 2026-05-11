package com.gitggal.clothesplz.config;

import com.gitggal.clothesplz.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

}
