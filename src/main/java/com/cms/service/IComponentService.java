package com.cms.service;

import java.util.List;

import com.cms.entity.Component;

public interface IComponentService {
  Component saveComponent(Component component, List<Long> pageIds, List<Long> bannerIds, List<Long> widgetIds);

  Boolean deleteComponent(Long id);

  Component getComponentById(Long id);

}
