package com.cms.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.controller.IWidgetController;
import com.cms.dto.DtoWidget;
import com.cms.dto.DtoWidgetIU;
import com.cms.entity.RootEntityResponse;
import com.cms.entity.Widget;
import com.cms.mapper.WidgetMapper;
import com.cms.service.IWidgetService;

@RestController
@RequestMapping("/api/v1/widgets")
public class WidgetController extends BaseController implements IWidgetController {

  @Autowired
  private IWidgetService widgetService;

  @Autowired
  private WidgetMapper widgetMapper;

  @Override
  @PostMapping
  public RootEntityResponse<DtoWidget> createWidget(@RequestBody DtoWidgetIU dtoWidgetIU) {
    Widget widget = widgetMapper.toWidget(dtoWidgetIU);
    Widget savedWidget = widgetService.saveWidget(widget, dtoWidgetIU.getBannerIds(), dtoWidgetIU.getPostIds());
    DtoWidget dtoWidget = widgetMapper.toDtoWidget(savedWidget);
    return ok(dtoWidget);
  }

  @Override
  @PutMapping("/{id}")
  public RootEntityResponse<DtoWidget> updateWidget(@PathVariable Long id, @RequestBody DtoWidgetIU dtoWidgetIU) {
    Widget widget = widgetService.getWidgetById(id);
    widgetMapper.updateWidgetFromDto(dtoWidgetIU, widget);
    Widget savedWidget = widgetService.saveWidget(widget, dtoWidgetIU.getBannerIds(), dtoWidgetIU.getPostIds());
    DtoWidget dtoWidget = widgetMapper.toDtoWidget(savedWidget);
    return ok(dtoWidget);
  }

  @Override
  @DeleteMapping("/{id}")
  public RootEntityResponse<Boolean> deleteWidget(@PathVariable Long id) {
    Boolean deleted = widgetService.deleteWidget(id);
    if (deleted) {
      return ok(deleted);
    }
    return error("Widget not deleted");
  }

  @Override
  @GetMapping("/{id}")
  public RootEntityResponse<DtoWidget> getWidgetById(@PathVariable Long id) {
    Widget widget = widgetService.getWidgetById(id);
    DtoWidget dtoWidget = widgetMapper.toDtoWidget(widget);
    return ok(dtoWidget);
  }

}
