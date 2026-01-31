package com.cms.controller;

import java.util.List;

import com.cms.dto.DtoPage;
import com.cms.dto.DtoPageDetail;
import com.cms.dto.DtoPageIU;
import com.cms.dto.DtoPageSummary;
import com.cms.dto.PagedResponse;
import com.cms.entity.RootEntityResponse;

public interface IPageController {
  RootEntityResponse<DtoPage> createPage(DtoPageIU dtoPageIU);

  RootEntityResponse<DtoPage> updatePage(Long id, DtoPageIU dtoPageIU);

  RootEntityResponse<Boolean> deletePage(Long id);

  RootEntityResponse<DtoPageDetail> getPageBySlug(String slug);

  RootEntityResponse<List<DtoPage>> getAllPages();

  RootEntityResponse<List<DtoPageSummary>> getAllPageSummary();

  // Paginated endpoints
  RootEntityResponse<PagedResponse<DtoPage>> getAllPagesPaged(int page, int size, String sort);

  RootEntityResponse<PagedResponse<DtoPageSummary>> getAllPageSummaryPaged(int page, int size, String sort);
}