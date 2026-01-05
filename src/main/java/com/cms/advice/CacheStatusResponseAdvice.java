package com.cms.advice;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class CacheStatusResponseAdvice implements ResponseBodyAdvice<Object> {

  private static final long CACHE_THRESHOLD_MS = 50;

  @Override
  public boolean supports(MethodParameter returnType, Class converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
      Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {

    if (request instanceof ServletServerHttpRequest) {
      HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
      Object startTimeObj = servletRequest.getAttribute("x-start-time");

      if (startTimeObj instanceof Long) {
        long startTime = (Long) startTimeObj;
        long duration = System.currentTimeMillis() - startTime;

        if (duration < CACHE_THRESHOLD_MS) {
          response.getHeaders().add("X-Cache-Status", "HIT");
        } else {
          response.getHeaders().add("X-Cache-Status", "MISS");
        }
      }
    }

    return body;
  }
}
