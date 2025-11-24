package com.cms.service;

import java.util.List;

import com.cms.entity.Widget;

public interface IWidgetService {
  Widget saveWidget(Widget widget, List<Long> bannerIds, List<Long> postIds);

  Boolean deleteWidget(Long id);

  Widget getWidgetById(Long id);

}
