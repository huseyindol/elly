package com.cms.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Autowired
  private RequestTimingInterceptor requestTimingInterceptor;

  @Override
  public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/assets/**")
        .addResourceLocations("file:assets/");
  }

  @Override
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    // Register timing interceptor for all REST API requests
    registry.addInterceptor(requestTimingInterceptor)
        .addPathPatterns("/api/**", "/oauth2/**", "/login/oauth2/**");
  }
}
