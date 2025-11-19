package com.cms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import com.cms.entity.Component;

public interface ComponentRepository extends JpaRepository<Component, Long> {
  @Override
  @EntityGraph(attributePaths = { "pages" })
  @NonNull
  Optional<Component> findById(@NonNull Long id);
}
