package com.gitggal.clothesplz.repository;

import org.flywaydb.core.Flyway;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DataJpaTest
@ActiveProfiles("test")
public abstract class RepositoryTestSupport {

  @MockitoBean
  protected Flyway flyway;
}
