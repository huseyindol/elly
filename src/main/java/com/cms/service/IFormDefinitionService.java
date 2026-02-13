package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cms.entity.FormDefinition;

public interface IFormDefinitionService {

  FormDefinition save(FormDefinition formDefinition);

  FormDefinition getById(Long id);

  List<FormDefinition> getAll();

  List<FormDefinition> getAllActive();

  Page<FormDefinition> getAllPaged(Pageable pageable);

  Page<FormDefinition> getAllActivePaged(Pageable pageable);

  Boolean delete(Long id);

  List<FormDefinition> searchByTitle(String title);
}
