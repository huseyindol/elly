package com.cms.controller.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.cms.entity.RootEntityResponse;

public class BaseController {
  public <T> RootEntityResponse<T> ok(T data) {
    return RootEntityResponse.ok(data);
  }

  public <T> RootEntityResponse<T> error(String message) {
    return RootEntityResponse.error(message);
  }

  protected Pageable createPageable(int page, int size, String sort) {
    String[] sortParams = sort.split(",");
    String sortField = sortParams[0];
    Sort.Direction direction = sortParams.length > 1
        && sortParams[1].equalsIgnoreCase("desc")
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;
    return PageRequest.of(page, size,
        Sort.by(direction, sortField));
  }
}
