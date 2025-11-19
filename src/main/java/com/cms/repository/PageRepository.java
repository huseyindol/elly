package com.cms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import com.cms.entity.Page;

public interface PageRepository extends JpaRepository<Page, Long> {
  @EntityGraph(attributePaths = { "components" })
  Optional<Page> findBySlug(String slug);

  @Override
  @EntityGraph(attributePaths = { "components" })
  @NonNull
  Optional<Page> findById(@NonNull Long id);
}
