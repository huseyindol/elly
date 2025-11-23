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

import com.cms.controller.IComponentController;
import com.cms.dto.DtoComponent;
import com.cms.dto.DtoComponentIU;
import com.cms.entity.Component;
import com.cms.entity.RootEntityResponse;
import com.cms.mapper.ComponentMapper;
import com.cms.service.IComponentService;

@RestController
@RequestMapping("/api/v1/components")
public class ComponentController extends BaseController implements IComponentController {

  @Autowired
  private IComponentService componentService;

  @Autowired
  private ComponentMapper componentMapper;

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

}
