package com.cms.service;

import java.util.List;

import com.cms.dto.DtoPageSummary;
import com.cms.entity.Page;

public interface IPageService {
  Page savePage(Page page);

  Boolean deletePage(Long id);

  Page getPageBySlug(String slug);

  Page getPageById(Long id);

  List<Page> getAllPages();

  List<DtoPageSummary> getAllPageSummary();
}
