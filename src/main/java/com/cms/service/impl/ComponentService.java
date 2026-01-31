package com.cms.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.DtoComponentSummary;
import com.cms.entity.Banner;
import com.cms.entity.Component;
import com.cms.entity.Page;
import com.cms.entity.Widget;
import com.cms.enums.ComponentTypeEnum;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.ValidationException;
import com.cms.repository.BannerRepository;
import com.cms.repository.ComponentRepository;
import com.cms.repository.PageRepository;
import com.cms.repository.WidgetRepository;
import com.cms.service.IComponentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ComponentService implements IComponentService {

  private final ComponentRepository componentRepository;
  private final BannerRepository bannerRepository;
  private final PageRepository pageRepository;
  private final WidgetRepository widgetRepository;

  @Override
  @Transactional
  @Caching(evict = {
      @CacheEvict(value = "components", allEntries = true),
      @CacheEvict(value = "pages", allEntries = true),
      @CacheEvict(value = "widgets", allEntries = true)
  })
  public Component saveComponent(Component component, List<Long> pageIds, List<Long> bannerIds, List<Long> widgetIds) {
    // ComponentTypeEnum validation
    ComponentTypeEnum componentType = component.getType();
    if (componentType == ComponentTypeEnum.BANNER && widgetIds != null && !widgetIds.isEmpty()) {
      throw new ValidationException("BANNER tipindeki component'e widget eklenemez");
    }
    if (componentType == ComponentTypeEnum.WIDGET && bannerIds != null && !bannerIds.isEmpty()) {
      throw new ValidationException("WIDGET tipindeki component'e banner eklenemez");
    }

    if (pageIds != null && !pageIds.isEmpty()) {
      Set<Page> pages = new HashSet<>(pageRepository.findAllById(pageIds));
      component.setPages(pages);
    } else {
      component.setPages(new HashSet<>());
    }
    if (bannerIds != null && !bannerIds.isEmpty()) {
      Set<Banner> banners = new HashSet<>(bannerRepository.findAllById(bannerIds));
      component.setBanners(banners);
    } else {
      component.setBanners(new HashSet<>());
    }
    if (widgetIds != null && !widgetIds.isEmpty()) {
      Set<Widget> widgets = new HashSet<>(widgetRepository.findAllById(widgetIds));
      component.setWidgets(widgets);
    } else {
      component.setWidgets(new HashSet<>());
    }
    return componentRepository.save(component);
  }

  @Override
  @Transactional
  @Caching(evict = {
      @CacheEvict(value = "components", allEntries = true),
      @CacheEvict(value = "pages", allEntries = true),
      @CacheEvict(value = "widgets", allEntries = true)
  })
  public Boolean deleteComponent(Long id) {
    if (componentRepository.existsById(id)) {
      componentRepository.deleteById(id);
      return true;
    }
    return false;
  }

  @Override
  @Cacheable(value = "components", key = "#id")
  public Component getComponentById(Long id) {
    return componentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Component", id));
  }

  @Override
  @Cacheable(value = "components", key = "'getAllComponents'")
  public List<Component> getAllComponents() {
    return componentRepository.findAllWithRelations();
  }

  @Override
  @Cacheable(value = "components", key = "'getAllComponentsSummary'")
  public List<DtoComponentSummary> getAllComponentsSummary() {
    return componentRepository.findAllWithSummary();
  }

  @Override
  public List<Component> getComponentsByIds(List<Long> ids) {
    return componentRepository.findAllById(ids);
  }

}
