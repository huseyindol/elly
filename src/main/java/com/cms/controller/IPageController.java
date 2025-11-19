package com.cms.controller;

import com.cms.dto.DtoPage;
import com.cms.dto.DtoPageIU;
import com.cms.entity.RootEntityResponse;

public interface IPageController {
  RootEntityResponse<DtoPage> createPage(DtoPageIU dtoPageIU);

  RootEntityResponse<DtoPage> updatePage(Long id, DtoPageIU dtoPageIU);

  RootEntityResponse<Boolean> deletePage(Long id);

  RootEntityResponse<DtoPage> getPageBySlug(String slug);

}