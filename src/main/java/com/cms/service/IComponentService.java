package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cms.dto.DtoComponentSummary;
import com.cms.entity.Component;

public interface IComponentService {
  Component saveComponent(Component component, List<Long> pageIds, List<Long> bannerIds, List<Long> widgetIds,
      List<Long> formIds);

  Boolean deleteComponent(Long id);

  Component getComponentById(Long id);

  List<Component> getAllComponents();

  List<DtoComponentSummary> getAllComponentsSummary();

  List<Component> getComponentsByIds(List<Long> ids);

  // Paginated methods
  Page<Component> getAllComponentsPaged(Pageable pageable);

  Page<DtoComponentSummary> getAllComponentsSummaryPaged(Pageable pageable);
}
