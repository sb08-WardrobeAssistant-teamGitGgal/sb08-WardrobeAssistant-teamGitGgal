package com.gitggal.clothesplz.support;

import com.gitggal.clothesplz.service.image.ImageUploader;
import org.flywaydb.core.Flyway;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

  @MockitoBean
  protected Flyway flyway;

  @MockitoBean
  protected ImageUploader imageUploader;

}
