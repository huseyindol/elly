package com.cms.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cms.entity.Banner;
import com.cms.entity.Widget;
import com.cms.repository.BannerRepository;
import com.cms.repository.WidgetRepository;
import com.cms.service.IWidgetService;

@Service
public class WidgetService implements IWidgetService {

  @Autowired
  private WidgetRepository widgetRepository;

  @Autowired
  private BannerRepository bannerRepository;

  @Override
  public Widget saveWidget(Widget widget, List<Long> bannerIds) {
    if (bannerIds != null && !bannerIds.isEmpty()) {
      List<Banner> banners = bannerRepository.findAllById(bannerIds);
      widget.setBanners(banners);
    } else {
      widget.setBanners(new ArrayList<>());
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
        .orElseThrow(() -> new RuntimeException("Widget not found"));
  }

}
