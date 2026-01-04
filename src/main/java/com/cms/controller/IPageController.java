package com.cms.controller;

import java.util.List;

import com.cms.dto.DtoPage;
import com.cms.dto.DtoPageIU;
import com.cms.dto.DtoPageSummary;
import com.cms.entity.RootEntityResponse;

public interface IPageController {
  RootEntityResponse<DtoPage> createPage(DtoPageIU dtoPageIU);

  RootEntityResponse<DtoPage> updatePage(Long id, DtoPageIU dtoPageIU);

  RootEntityResponse<Boolean> deletePage(Long id);

  RootEntityResponse<DtoPage> getPageBySlug(String slug);

  RootEntityResponse<List<DtoPage>> getAllPages();

  RootEntityResponse<List<DtoPageSummary>> getAllPageSummary();

}