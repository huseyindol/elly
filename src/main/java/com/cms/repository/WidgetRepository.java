package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import com.cms.dto.DtoWidgetSummary;
import com.cms.entity.Widget;

public interface WidgetRepository extends JpaRepository<Widget, Long> {
  @Override
  @EntityGraph(attributePaths = { "banners", "posts" })
  @NonNull
  Optional<Widget> findById(@NonNull Long id);

  @Query("SELECT w FROM Widget w")
  @EntityGraph(attributePaths = { "banners", "posts" })
  List<Widget> findAllWithRelations();

  @Query("SELECT new com.cms.dto.DtoWidgetSummary(w.id, w.name, w.type, w.orderIndex, w.status) FROM Widget w")
  List<DtoWidgetSummary> findAllWithSummary();

  // Paginated methods
  @Query("SELECT w FROM Widget w")
  @EntityGraph(attributePaths = { "banners", "posts" })
  Page<Widget> findAllWithRelationsPaged(Pageable pageable);

  @Query(value = "SELECT new com.cms.dto.DtoWidgetSummary(w.id, w.name, w.type, w.orderIndex, w.status) FROM Widget w", countQuery = "SELECT count(w) FROM Widget w")
  Page<DtoWidgetSummary> findAllWithSummaryPaged(Pageable pageable);
}
