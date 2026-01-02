package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import com.cms.entity.Page;

public interface PageRepository extends JpaRepository<Page, Long> {
  @EntityGraph(attributePaths = { "components", "seoInfo" })
  Optional<Page> findBySlug(String slug);

  @Override
  @EntityGraph(attributePaths = { "components", "seoInfo" })
  @NonNull
  Optional<Page> findById(@NonNull Long id);

  @Query("SELECT p FROM Page p")
  @EntityGraph(attributePaths = { "seoInfo" })
  List<Page> findAllWithRelations();
}
