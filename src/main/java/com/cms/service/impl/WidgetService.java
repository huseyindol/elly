package com.cms.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

@Service
public class WidgetService implements IWidgetService {

  @Autowired
  private WidgetRepository widgetRepository;

  @Autowired
  private BannerRepository bannerRepository;

  @Autowired
  private PostRepository postRepository;

  @Override
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
      List<Banner> banners = bannerRepository.findAllById(bannerIds);
      widget.setBanners(banners);
    } else {
      widget.setBanners(new ArrayList<>());
    }
    if (postIds != null && !postIds.isEmpty()) {
      List<Post> posts = postRepository.findAllById(postIds);
      widget.setPosts(posts);
    } else {
      widget.setPosts(new ArrayList<>());
    }
    return widgetRepository.save(widget);
  }

  @Override
  public Boolean deleteWidget(Long id) {
    if (widgetRepository.existsById(id)) {
      widgetRepository.deleteById(id);
      return true;
    }
    return false;
  }

  @Override
  public Widget getWidgetById(Long id) {
    return widgetRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Widget", id));
  }

}
