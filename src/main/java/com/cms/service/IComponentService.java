package com.cms.service;

import java.util.List;

import com.cms.dto.DtoComponentSummary;
import com.cms.entity.Component;

public interface IComponentService {
  Component saveComponent(Component component, List<Long> pageIds, List<Long> bannerIds, List<Long> widgetIds);

  Boolean deleteComponent(Long id);

  Component getComponentById(Long id);

  List<Component> getAllComponents();

  List<DtoComponentSummary> getAllComponentsSummary();

  List<Component> getComponentsByIds(List<Long> ids);

}
