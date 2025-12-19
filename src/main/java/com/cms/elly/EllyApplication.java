package com.cms.elly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = { "com.cms" })
@EntityScan(basePackages = { "com.cms" })
@ComponentScan(basePackages = { "com.cms" })
@EnableJpaAuditing
public class EllyApplication {

  public static void main(String[] args) {
    SpringApplication.run(EllyApplication.class, args);
  }

}
