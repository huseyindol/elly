package com.cms.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cms.entity.Page;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.PageRepository;
import com.cms.service.IPageService;

@Service
public class PageService implements IPageService {

  @Autowired
  private PageRepository pageRepository;

  @Override
  public Page savePage(Page page) {
    return pageRepository.save(page);
  }

  @Override
  public Boolean deletePage(Long id) {
    if (pageRepository.existsById(id)) {
      pageRepository.deleteById(id);
      return true;
    }
    return false;
  }

  @Override
  public Page getPageBySlug(String slug) {
    return pageRepository.findBySlug(slug)
        .orElseThrow(() -> new ResourceNotFoundException("Page", "slug", slug));
  }

  @Override
  public Page getPageById(Long id) {
    return pageRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Page", id));
  }

}