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

import com.cms.controller.IComponentController;
import com.cms.dto.DtoComponent;
import com.cms.dto.DtoComponentIU;
import com.cms.dto.DtoComponentSummary;
import com.cms.dto.PagedResponse;
import com.cms.entity.Component;
import com.cms.entity.RootEntityResponse;
import com.cms.mapper.ComponentMapper;
import com.cms.service.IComponentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/components")
@RequiredArgsConstructor
public class ComponentController extends BaseController implements IComponentController {

  private final IComponentService componentService;
  private final ComponentMapper componentMapper;

  @Override
  @PostMapping
  public RootEntityResponse<DtoComponent> createComponent(@RequestBody DtoComponentIU dtoComponentIU) {
    Component component = componentMapper.toComponent(dtoComponentIU);
    Component savedComponent = componentService.saveComponent(component, dtoComponentIU.getPageIds(),
        dtoComponentIU.getBannerIds(), dtoComponentIU.getWidgetIds());
    DtoComponent dtoComponent = componentMapper.toDtoComponent(savedComponent);
    return ok(dtoComponent);
  }

  @Override
  @PutMapping("/{id}")
  public RootEntityResponse<DtoComponent> updateComponent(@PathVariable Long id,
      @RequestBody DtoComponentIU dtoComponentIU) {
    Component component = componentService.getComponentById(id);
    componentMapper.updateComponentFromDto(dtoComponentIU, component);
    Component savedComponent = componentService.saveComponent(component, dtoComponentIU.getPageIds(),
        dtoComponentIU.getBannerIds(), dtoComponentIU.getWidgetIds());
    DtoComponent dtoComponent = componentMapper.toDtoComponent(savedComponent);
    return ok(dtoComponent);
  }

  @Override
  @DeleteMapping("/{id}")
  public RootEntityResponse<Boolean> deleteComponent(@PathVariable Long id) {
    Boolean deleted = componentService.deleteComponent(id);
    if (deleted) {
      return ok(deleted);
    }
    return error("Component not deleted");
  }

  @Override
  @GetMapping("/{id}")
  public RootEntityResponse<DtoComponent> getComponentById(@PathVariable Long id) {
    Component component = componentService.getComponentById(id);
    DtoComponent dtoComponent = componentMapper.toDtoComponent(component);
    return ok(dtoComponent);
  }

  @Override
  @GetMapping("/list")
  public RootEntityResponse<List<DtoComponent>> getAllComponents() {
    List<Component> components = componentService.getAllComponents();
    List<DtoComponent> dtoComponents = componentMapper.toDtoComponentListSimple(components);
    return ok(dtoComponents);
  }

  @Override
  @GetMapping("/list/summary")
  public RootEntityResponse<List<DtoComponentSummary>> getAllComponentsSummary() {
    List<DtoComponentSummary> dtoComponentSummary = componentService.getAllComponentsSummary();
    return ok(dtoComponentSummary);
  }

  // Paginated endpoints
  @Override
  @GetMapping("/list/paged")
  public RootEntityResponse<PagedResponse<DtoComponent>> getAllComponentsPaged(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id,asc") String sort) {
    Pageable pageable = createPageable(page, size, sort);
    Page<Component> pageResult = componentService.getAllComponentsPaged(pageable);
    List<DtoComponent> dtoComponents = componentMapper.toDtoComponentListSimple(pageResult.getContent());
    return ok(PagedResponse.from(pageResult, dtoComponents));
  }

  @Override
  @GetMapping("/list/summary/paged")
  public RootEntityResponse<PagedResponse<DtoComponentSummary>> getAllComponentsSummaryPaged(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "id,asc") String sort) {
    Pageable pageable = createPageable(page, size, sort);
    Page<DtoComponentSummary> pageResult = componentService.getAllComponentsSummaryPaged(pageable);
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
