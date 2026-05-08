package com.gitggal.clothesplz.config;

import com.gitggal.clothesplz.service.image.S3Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableJpaAuditing
@EnableScheduling
@EnableConfigurationProperties(S3Properties.class)
public class AppConfig {

}
