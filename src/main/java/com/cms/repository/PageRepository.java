package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import com.cms.dto.DtoPageSummary;
import com.cms.entity.Page;

public interface PageRepository extends JpaRepository<Page, Long> {
  @EntityGraph(attributePaths = { "components", "components.banners", "components.widgets", "components.widgets.posts",
      "components.widgets.banners", "seoInfo" })
  Optional<Page> findBySlug(String slug);

  @Override
  @EntityGraph(attributePaths = { "components", "seoInfo" })
  @NonNull
  Optional<Page> findById(@NonNull Long id);

  @Query("SELECT p FROM Page p")
  @EntityGraph(attributePaths = { "seoInfo" })
  List<Page> findAllWithRelations();

  @Query("SELECT new com.cms.dto.DtoPageSummary(p.id, p.title, p.slug, p.status) FROM Page p")
  List<DtoPageSummary> findAllWithSummary();

  // Paginated methods - use fully qualified name to avoid collision with entity
  // Page
  @Query("SELECT p FROM Page p")
  @EntityGraph(attributePaths = { "seoInfo" })
  org.springframework.data.domain.Page<Page> findAllWithRelationsPaged(Pageable pageable);

  @Query(value = "SELECT new com.cms.dto.DtoPageSummary(p.id, p.title, p.slug, p.status) FROM Page p", countQuery = "SELECT count(p) FROM Page p")
  org.springframework.data.domain.Page<DtoPageSummary> findAllWithSummaryPaged(Pageable pageable);
}
