package com.cms.config;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * RabbitMQ Management HTTP API (default :15672/api) icin synchronous RestClient.
 * Basic auth + timeout ile konfiguredir. WebFlux eklememek icin RestClient (Spring 6.1+)
 * kullanildi — sync cagrilar icin webflux-starter'a gerek yok.
 */
@Slf4j
@Configuration
public class RabbitMgmtClientConfig {

  @Value("${rabbitmq.mgmt.url}")
  private String baseUrl;

  @Value("${spring.rabbitmq.username}")
  private String username;

  @Value("${spring.rabbitmq.password}")
  private String password;

  @Value("${rabbitmq.mgmt.connect-timeout-ms:2000}")
  private int connectTimeoutMs;

  @Value("${rabbitmq.mgmt.read-timeout-ms:5000}")
  private int readTimeoutMs;

  @Bean("rabbitMgmtRestClient")
  public RestClient rabbitMgmtRestClient(RestClient.Builder builder) {
    log.info("RabbitMQ management RestClient initialized: url={}, user={}", baseUrl, username);

    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
    requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

    // EncodingMode.NONE: RabbitAdminService already percent-encodes all path
    // segments (e.g. vhost "/" → "%2F"). Letting Spring re-encode would produce
    // double-encoded values like "%252F", causing 404 from the management API.
    DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory(baseUrl);
    uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

    return builder
        .uriBuilderFactory(uriBuilderFactory)
        .requestFactory(requestFactory)
        .defaultHeaders(headers -> {
          headers.setBasicAuth(username, password);
          headers.setAccept(List.of(MediaType.APPLICATION_JSON));
          headers.set(HttpHeaders.USER_AGENT, "elly-cms-rabbit-admin");
        })
        .build();
  }
}
