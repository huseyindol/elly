package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import com.cms.entity.Widget;

public interface WidgetRepository extends JpaRepository<Widget, Long> {
  @Override
  @EntityGraph(attributePaths = { "banners", "posts" })
  @NonNull
  Optional<Widget> findById(@NonNull Long id);

  @Query("SELECT w FROM Widget w")
  @EntityGraph(attributePaths = { "banners", "posts" })
  List<Widget> findAllWithRelations();
}
