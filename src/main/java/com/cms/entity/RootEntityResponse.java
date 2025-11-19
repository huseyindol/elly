package com.cms.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RootEntityResponse<T> {
  private boolean result;
  private String message;
  private T data;

  public static <T> RootEntityResponse<T> ok(T data) {
    RootEntityResponse<T> response = new RootEntityResponse<>();
    response.setResult(true);
    response.setMessage(null);
    response.setData(data);
    return response;
  }

  public static <T> RootEntityResponse<T> error(String message) {
    RootEntityResponse<T> response = new RootEntityResponse<>();
    response.setResult(false);
    response.setMessage(message);
    response.setData(null);
    return response;
  }
}
