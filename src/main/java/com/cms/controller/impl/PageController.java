package com.cms.controller.impl;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.controller.IPageController;
import com.cms.dto.DtoPage;
import com.cms.dto.DtoPageIU;
import com.cms.dto.DtoPageSummary;
import com.cms.entity.Page;
import com.cms.entity.RootEntityResponse;
import com.cms.mapper.PageMapper;
import com.cms.service.IPageService;
import com.cms.service.IComponentService;
import com.cms.entity.Component;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pages")
@RequiredArgsConstructor
public class PageController extends BaseController implements IPageController {

  private final IPageService pageService;
  private final PageMapper pageMapper;
  private final IComponentService componentService;

  @Override
  @PostMapping
  public RootEntityResponse<DtoPage> createPage(@RequestBody DtoPageIU dtoPageIU) {
    Page page = pageMapper.toPage(dtoPageIU);

    if (dtoPageIU.getComponentIds() != null && !dtoPageIU.getComponentIds().isEmpty()) {
      List<Component> components = componentService.getComponentsByIds(dtoPageIU.getComponentIds());
      page.setComponents(new java.util.LinkedHashSet<>(components));
    }

    Page savedPage = pageService.savePage(page);
    DtoPage dtoPage = pageMapper.toDtoPage(savedPage);
    return ok(dtoPage);
  }

  @Override
  @PutMapping("/{id}")
  public RootEntityResponse<DtoPage> updatePage(@PathVariable Long id, @RequestBody DtoPageIU dtoPageIU) {
    Page page = pageService.getPageById(id);
    pageMapper.updatePageFromDto(dtoPageIU, page);

    if (dtoPageIU.getComponentIds() != null) {
      if (dtoPageIU.getComponentIds().isEmpty()) {
        page.getComponents().clear();
      } else {
        List<Component> components = componentService.getComponentsByIds(dtoPageIU.getComponentIds());
        page.setComponents(new java.util.LinkedHashSet<>(components));
      }
    }

    Page savedPage = pageService.savePage(page);
    DtoPage dtoPage = pageMapper.toDtoPage(savedPage);
    return ok(dtoPage);
  }

  @Override
  @DeleteMapping("/{id}")
  public RootEntityResponse<Boolean> deletePage(@PathVariable Long id) {
    Boolean deleted = pageService.deletePage(id);
    if (deleted) {
      return ok(deleted);
    }
    return error("Page not deleted");
  }

  @Override
  @GetMapping("/{slug}")
  public RootEntityResponse<DtoPage> getPageBySlug(@PathVariable String slug) {
    Page pageBySlug = pageService.getPageBySlug(slug);
    DtoPage dtoPage = pageMapper.toDtoPage(pageBySlug);
    return ok(dtoPage);
  }

  @Override
  @GetMapping("/list")
  public RootEntityResponse<List<DtoPage>> getAllPages() {
    List<Page> pages = pageService.getAllPages();
    List<DtoPage> dtoPages = pageMapper.toDtoPageListSimple(pages);
    return ok(dtoPages);
  }

  @Override
  @GetMapping("/list/summary")
  public RootEntityResponse<List<DtoPageSummary>> getAllPageSummary() {
    List<DtoPageSummary> pages = pageService.getAllPageSummary();
    return ok(pages);
  }
}
