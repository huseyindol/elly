package com.cms.service;

import java.util.List;

import com.cms.entity.Component;

public interface IComponentService {
  Component saveComponent(Component component);

  Component saveComponentWithPages(Component component, List<Long> pageIds);

  Boolean deleteComponent(Long id);

  Component getComponentById(Long id);

}
