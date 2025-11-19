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

import com.cms.controller.IPageController;
import com.cms.dto.DtoPage;
import com.cms.dto.DtoPageIU;
import com.cms.entity.Page;
import com.cms.entity.RootEntityResponse;
import com.cms.mapper.PageMapper;
import com.cms.service.IPageService;

@RestController
@RequestMapping("/api/v1/pages")
public class PageController extends BaseController implements IPageController {

  @Autowired
  private IPageService pageService;
  @Autowired
  private PageMapper pageMapper;

  @Override
  @PostMapping
  public RootEntityResponse<DtoPage> createPage(@RequestBody DtoPageIU dtoPageIU) {
    Page page = pageMapper.toPage(dtoPageIU);
    Page savedPage = pageService.savePage(page);
    DtoPage dtoPage = pageMapper.toDtoPage(savedPage);
    return ok(dtoPage);
  }

  @Override
  @PutMapping("/{id}")
  public RootEntityResponse<DtoPage> updatePage(@PathVariable Long id, @RequestBody DtoPageIU dtoPageIU) {
    Page page = pageService.getPageById(id);
    pageMapper.updatePageFromDto(dtoPageIU, page);
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
}
