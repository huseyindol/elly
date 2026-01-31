package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cms.dto.DtoWidgetSummary;
import com.cms.entity.Widget;

public interface IWidgetService {
  Widget saveWidget(Widget widget, List<Long> bannerIds, List<Long> postIds);

  Boolean deleteWidget(Long id);

  Widget getWidgetById(Long id);

  List<Widget> getAllWidgets();

  List<DtoWidgetSummary> getAllWidgetsSummary();

  // Paginated methods
  Page<Widget> getAllWidgetsPaged(Pageable pageable);

  Page<DtoWidgetSummary> getAllWidgetsSummaryPaged(Pageable pageable);
}
