package com.cms.service;

import com.cms.entity.Page;

public interface IPageService {
  Page savePage(Page page);

  Boolean deletePage(Long id);

  Page getPageBySlug(String slug);

  Page getPageById(Long id);
}
