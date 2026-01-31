package com.cms.controller.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.controller.IWidgetController;
import com.cms.dto.DtoWidget;
import com.cms.dto.DtoWidgetIU;
import com.cms.dto.DtoWidgetSummary;
import com.cms.dto.PagedResponse;
import com.cms.entity.RootEntityResponse;
import com.cms.entity.Widget;
import com.cms.mapper.WidgetMapper;
import com.cms.service.IWidgetService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/widgets")
@RequiredArgsConstructor
public class WidgetController extends BaseController implements IWidgetController {

  private final IWidgetService widgetService;
  private final WidgetMapper widgetMapper;

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

  @Override
  @GetMapping("/list")
  public RootEntityResponse<List<DtoWidget>> getAllWidgets() {
    List<Widget> widgets = widgetService.getAllWidgets();
    List<DtoWidget> dtoWidgets = widgetMapper.toDtoWidgetListSimple(widgets);
    return ok(dtoWidgets);
  }

  @Override
  @GetMapping("/list/summary")
  public RootEntityResponse<List<DtoWidgetSummary>> getAllWidgetsSummary() {
    List<DtoWidgetSummary> dtoWidgetSummaries = widgetService.getAllWidgetsSummary();
    return ok(dtoWidgetSummaries);
  }

  // Paginated endpoints
  @Override
  @GetMapping("/list/paged")
  public RootEntityResponse<PagedResponse<DtoWidget>> getAllWidgetsPaged(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id,asc") String sort) {
    Pageable pageable = createPageable(page, size, sort);
    Page<Widget> pageResult = widgetService.getAllWidgetsPaged(pageable);
    List<DtoWidget> dtoWidgets = widgetMapper.toDtoWidgetListSimple(pageResult.getContent());
    return ok(PagedResponse.from(pageResult, dtoWidgets));
  }

  @Override
  @GetMapping("/list/summary/paged")
  public RootEntityResponse<PagedResponse<DtoWidgetSummary>> getAllWidgetsSummaryPaged(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id,asc") String sort) {
    Pageable pageable = createPageable(page, size, sort);
    Page<DtoWidgetSummary> pageResult = widgetService.getAllWidgetsSummaryPaged(pageable);
    return ok(PagedResponse.from(pageResult));
  }

  private Pageable createPageable(int page, int size, String sort) {
    String[] sortParams = sort.split(",");
    String sortField = sortParams[0];
    Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc")
        ? Sort.Direction.DESC
        : Sort.Direction.ASC;
    return PageRequest.of(page, size, Sort.by(direction, sortField));
  }
}
