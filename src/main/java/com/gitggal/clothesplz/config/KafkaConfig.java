package com.gitggal.clothesplz.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

// TODO: kafka 배포 환경에 맞춰 브로커 수와 replicas 맞춰야함
@Profile("prod")
@Configuration
public class KafkaConfig {

  @Bean
  public NewTopic feedEsSyncTopic() {
    return TopicBuilder.name("feed-es-sync")
        .partitions(3)
        .replicas(3)
        .build();
  }

  @Bean
  public NewTopic feedEsDeleteTopic() {
    return TopicBuilder.name("feed-es-delete")
        .partitions(3)
        .replicas(3)
        .build();
  }
}
