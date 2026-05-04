package com.gitggal.clothesplz.support;

import org.flywaydb.core.Flyway;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

  @MockitoBean
  protected Flyway flyway;

}
