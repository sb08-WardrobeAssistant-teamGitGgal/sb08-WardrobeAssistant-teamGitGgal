package com.gitggal.clothesplz.controller;

import com.gitggal.clothesplz.support.IntegrationTestSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public abstract class ControllerTestSupport extends IntegrationTestSupport {

  @Autowired
  protected MockMvc mockMvc;

}
