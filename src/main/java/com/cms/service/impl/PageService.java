package com.cms.service.impl;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.DtoPageSummary;
import com.cms.entity.Page;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.PageRepository;
import com.cms.service.IPageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PageService implements IPageService {

  private final PageRepository pageRepository;

  @Override
  @Transactional
  @Caching(evict = {
      @CacheEvict(value = "pages", allEntries = true),
      @CacheEvict(value = "components", allEntries = true)
  })
  public Page savePage(Page page) {
    return pageRepository.save(page);
  }

  @Override
  @Transactional
  @Caching(evict = {
      @CacheEvict(value = "pages", allEntries = true),
      @CacheEvict(value = "components", allEntries = true)
  })
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

  // Paginated methods
  @Override
  @Cacheable(value = "pages", key = "'getAllPagesPaged_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
  public org.springframework.data.domain.Page<Page> getAllPagesPaged(Pageable pageable) {
    return pageRepository.findAllWithRelationsPaged(pageable);
  }

  @Override
  @Cacheable(value = "pages", key = "'getAllPageSummaryPaged_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
  public org.springframework.data.domain.Page<DtoPageSummary> getAllPageSummaryPaged(Pageable pageable) {
    return pageRepository.findAllWithSummaryPaged(pageable);
  }
}