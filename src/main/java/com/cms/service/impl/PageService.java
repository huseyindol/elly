package com.cms.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.DtoPageSummary;
import com.cms.entity.Page;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.PageRepository;
import com.cms.service.IPageService;

@Service
public class PageService implements IPageService {

  @Autowired
  private PageRepository pageRepository;

  @Override
  @Transactional
  @CacheEvict(value = "pages", allEntries = true)
  public Page savePage(Page page) {
    return pageRepository.save(page);
  }

  @Override
  @Transactional
  @CacheEvict(value = "pages", allEntries = true)
  public Boolean deletePage(Long id) {
    if (pageRepository.existsById(id)) {
      pageRepository.deleteById(id);
      return true;
    }
    return false;
  }

  @Override
  @Cacheable(value = "pages", key = "#slug")
  public Page getPageBySlug(String slug) {
    return pageRepository.findBySlug(slug)
        .orElseThrow(() -> new ResourceNotFoundException("Page", "slug", slug));
  }

  @Override
  @Cacheable(value = "pages", key = "#id")
  public Page getPageById(Long id) {
    return pageRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Page", id));
  }

  @Override
  @Cacheable(value = "pages", key = "'getAllPages'")
  public List<Page> getAllPages() {
    return pageRepository.findAllWithRelations();
  }

  @Override
  @Cacheable(value = "pages", key = "'getAllPageSummary'")
  public List<DtoPageSummary> getAllPageSummary() {
    List<DtoPageSummary> pageSummary = pageRepository.findAllWithSummary();
    return pageSummary;
  }

}