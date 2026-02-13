package com.cms.service.impl;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.entity.FormDefinition;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.FormDefinitionRepository;
import com.cms.service.IFormDefinitionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FormDefinitionService implements IFormDefinitionService {

  private final FormDefinitionRepository formDefinitionRepository;

  @Override
  @Transactional
  @Caching(evict = {
      @CacheEvict(value = "formDefinitions", allEntries = true),
      @CacheEvict(value = "formSubmissions", allEntries = true)
  })
  public FormDefinition save(FormDefinition formDefinition) {
    return formDefinitionRepository.save(formDefinition);
  }

  @Override
  @Cacheable(value = "formDefinitions", key = "#id")
  public FormDefinition getById(Long id) {
    return formDefinitionRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("FormDefinition", id));
  }

  @Override
  @Cacheable(value = "formDefinitions", key = "'getAllFormDefinitions'")
  public List<FormDefinition> getAll() {
    return formDefinitionRepository.findAll();
  }

  @Override
  @Cacheable(value = "formDefinitions", key = "'getAllActiveFormDefinitions'")
  public List<FormDefinition> getAllActive() {
    return formDefinitionRepository.findAllActive();
  }

  @Override
  @Cacheable(value = "formDefinitions", key = "'getAllFormDefinitionsPaged_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
  public Page<FormDefinition> getAllPaged(Pageable pageable) {
    return formDefinitionRepository.findAll(pageable);
  }

  @Override
  @Cacheable(value = "formDefinitions", key = "'getAllActiveFormDefinitionsPaged_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
  public Page<FormDefinition> getAllActivePaged(Pageable pageable) {
    return formDefinitionRepository.findAllActivePaged(pageable);
  }

  @Override
  @Transactional
  @Caching(evict = {
      @CacheEvict(value = "formDefinitions", allEntries = true),
      @CacheEvict(value = "formSubmissions", allEntries = true)
  })
  public Boolean delete(Long id) {
    if (formDefinitionRepository.existsById(id)) {
      formDefinitionRepository.deleteById(id);
      return true;
    }
    return false;
  }

  @Override
  public List<FormDefinition> searchByTitle(String title) {
    return formDefinitionRepository.findByTitleContainingIgnoreCase(title);
  }
}
