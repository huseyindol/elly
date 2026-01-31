package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.cms.dto.DtoPageSummary;
import com.cms.entity.Page;

public interface IPageService {
  Page savePage(Page page);

  Boolean deletePage(Long id);

  Page getPageBySlug(String slug);

  Page getPageById(Long id);

  List<Page> getAllPages();

  List<DtoPageSummary> getAllPageSummary();

  // Paginated methods
  org.springframework.data.domain.Page<Page> getAllPagesPaged(Pageable pageable);

  org.springframework.data.domain.Page<DtoPageSummary> getAllPageSummaryPaged(Pageable pageable);
}
