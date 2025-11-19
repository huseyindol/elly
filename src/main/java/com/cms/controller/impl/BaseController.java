package com.cms.controller.impl;

import com.cms.entity.RootEntityResponse;

public class BaseController {
  public <T> RootEntityResponse<T> ok(T data) {
    return RootEntityResponse.ok(data);
  }

  public <T> RootEntityResponse<T> error(String message) {
    return RootEntityResponse.error(message);
  }
}
