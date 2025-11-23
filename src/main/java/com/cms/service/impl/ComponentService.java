package com.cms.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cms.entity.Banner;
import com.cms.entity.Component;
import com.cms.entity.Page;
import com.cms.entity.Widget;
import com.cms.repository.BannerRepository;
import com.cms.repository.ComponentRepository;
import com.cms.repository.PageRepository;
import com.cms.repository.WidgetRepository;
import com.cms.service.IComponentService;

@Service
public class ComponentService implements IComponentService {

  @Autowired
  private ComponentRepository componentRepository;

  @Autowired
  private BannerRepository bannerRepository;

  @Autowired
  private PageRepository pageRepository;

  @Autowired
  private WidgetRepository widgetRepository;

  @Override
  public Component saveComponent(Component component, List<Long> pageIds, List<Long> bannerIds, List<Long> widgetIds) {
    if (pageIds != null && !pageIds.isEmpty()) {
      List<Page> pages = pageRepository.findAllById(pageIds);
      component.setPages(pages);
    } else {
      component.setPages(new ArrayList<>());
    }
    if (bannerIds != null && !bannerIds.isEmpty()) {
      List<Banner> banners = bannerRepository.findAllById(bannerIds);
      component.setBanners(banners);
    } else {
      component.setBanners(new ArrayList<>());
    }
    if (widgetIds != null && !widgetIds.isEmpty()) {
      List<Widget> widgets = widgetRepository.findAllById(widgetIds);
      component.setWidgets(widgets);
    } else {
      component.setWidgets(new ArrayList<>());
    }
    return componentRepository.save(component);
  }

  @Override
  public Boolean deleteComponent(Long id) {
    if (componentRepository.existsById(id)) {
      componentRepository.deleteById(id);
      return true;
    }
    return false;
  }

  @Override
  public Component getComponentById(Long id) {
    Component component = componentRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Component not found"));
    return component;
  }

}
