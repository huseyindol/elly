package com.cms.elly;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {"com.cms"})
@EntityScan(basePackages = {"com.cms"})
@ComponentScan(basePackages = {"com.cms"})
@EnableJpaAuditing
@OpenAPIDefinition(info = @Info(title = "Elly CMS API", version = "1.0", description = "API documentation for " +
  "Elly CMS"))
public class EllyApplication {

  public static void main(String[] args) {
    SpringApplication.run(EllyApplication.class, args);
  }

}
