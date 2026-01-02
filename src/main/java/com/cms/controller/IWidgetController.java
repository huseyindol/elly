package com.cms.controller;

import java.util.List;

import com.cms.dto.DtoWidget;
import com.cms.dto.DtoWidgetIU;
import com.cms.entity.RootEntityResponse;

public interface IWidgetController {
  RootEntityResponse<DtoWidget> createWidget(DtoWidgetIU dtoWidgetIU);

  RootEntityResponse<DtoWidget> updateWidget(Long id, DtoWidgetIU dtoWidgetIU);

  RootEntityResponse<Boolean> deleteWidget(Long id);

  RootEntityResponse<DtoWidget> getWidgetById(Long id);

  RootEntityResponse<List<DtoWidget>> getAllWidgets();
}
