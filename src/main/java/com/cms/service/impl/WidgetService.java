package com.cms.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.DtoWidgetSummary;
import com.cms.entity.Banner;
import com.cms.entity.Post;
import com.cms.entity.Widget;
import com.cms.enums.WidgetTypeEnum;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.ValidationException;
import com.cms.repository.BannerRepository;
import com.cms.repository.PostRepository;
import com.cms.repository.WidgetRepository;
import com.cms.service.IWidgetService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WidgetService implements IWidgetService {

  private final WidgetRepository widgetRepository;
  private final BannerRepository bannerRepository;
  private final PostRepository postRepository;

  @Override
  @Transactional
  @Caching(evict = {
      @CacheEvict(value = "widgets", allEntries = true),
      @CacheEvict(value = "components", allEntries = true)
  })
  public Widget saveWidget(Widget widget, List<Long> bannerIds, List<Long> postIds) {
    // WidgetTypeEnum validation
    WidgetTypeEnum widgetType = widget.getType();
    if (widgetType == WidgetTypeEnum.BANNER && postIds != null && !postIds.isEmpty()) {
      throw new ValidationException("BANNER tipindeki widget'a post eklenemez");
    }
    if (widgetType == WidgetTypeEnum.POST && bannerIds != null && !bannerIds.isEmpty()) {
      throw new ValidationException("POST tipindeki widget'a banner eklenemez");
    }

    if (bannerIds != null && !bannerIds.isEmpty()) {
      Set<Banner> banners = new HashSet<>(bannerRepository.findAllById(bannerIds));
      widget.setBanners(banners);
    } else {
      widget.setBanners(new HashSet<>());
    }
    if (postIds != null && !postIds.isEmpty()) {
      Set<Post> posts = new HashSet<>(postRepository.findAllById(postIds));
      widget.setPosts(posts);
    } else {
      widget.setPosts(new HashSet<>());
    }
    return widgetRepository.save(widget);
  }

  @Override
  @Transactional
  @Caching(evict = {
      @CacheEvict(value = "widgets", allEntries = true),
      @CacheEvict(value = "components", allEntries = true)
  })
  public Boolean deleteWidget(Long id) {
    if (widgetRepository.existsById(id)) {
      widgetRepository.deleteById(id);
      return true;
    }
    return false;
  }

  @Override
  @Cacheable(value = "widgets", key = "#id")
  public Widget getWidgetById(Long id) {
    return widgetRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Widget", id));
  }

  @Override
  @Cacheable(value = "widgets", key = "'getAllWidgets'")
  public List<Widget> getAllWidgets() {
    return widgetRepository.findAllWithRelations();
  }

  @Override
  @Cacheable(value = "widgets", key = "'getAllWidgetsSummary'")
  public List<DtoWidgetSummary> getAllWidgetsSummary() {
    return widgetRepository.findAllWithSummary();
  }

}
