package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.entity.FormDefinition;

public interface FormDefinitionRepository extends JpaRepository<FormDefinition, Long> {

  List<FormDefinition> findByActiveTrue();

  Optional<FormDefinition> findByTitleAndVersion(String title, Integer version);

  @Query("SELECT f FROM FormDefinition f WHERE f.active = true ORDER BY f.title")
  List<FormDefinition> findAllActive();

  @Query("SELECT f FROM FormDefinition f WHERE f.active = true")
  Page<FormDefinition> findAllActivePaged(Pageable pageable);

  @Query("SELECT f FROM FormDefinition f WHERE LOWER(f.title) LIKE LOWER(CONCAT('%', :title, '%'))")
  List<FormDefinition> findByTitleContainingIgnoreCase(@Param("title") String title);
}
