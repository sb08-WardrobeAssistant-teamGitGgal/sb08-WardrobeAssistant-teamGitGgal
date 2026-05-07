package com.gitggal.clothesplz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.gitggal.clothesplz")
public class ClothesplzApplication {

  public static void main(String[] args) {
    SpringApplication.run(ClothesplzApplication.class, args);
  }

}
