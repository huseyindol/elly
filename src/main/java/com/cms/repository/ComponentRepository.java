package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import com.cms.dto.DtoComponentSummary;
import com.cms.entity.Component;

public interface ComponentRepository extends JpaRepository<Component, Long> {
  @Override
  @EntityGraph(attributePaths = { "pages", "banners", "widgets" })
  @NonNull
  Optional<Component> findById(@NonNull Long id);

  @Query("SELECT c FROM Component c")
  @EntityGraph(attributePaths = { "pages", "banners", "widgets" })
  List<Component> findAllWithRelations();

  @Query("SELECT new com.cms.dto.DtoComponentSummary(c.id, c.name, c.status, c.type, c.orderIndex) FROM Component c")
  List<DtoComponentSummary> findAllWithSummary();
}
