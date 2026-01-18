package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import com.cms.dto.DtoPostSummary;
import com.cms.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {
  @Override
  @EntityGraph(attributePaths = { "seoInfo" })
  @NonNull
  Optional<Post> findById(@NonNull Long id);

  @Query("SELECT p FROM Post p")
  @EntityGraph(attributePaths = { "seoInfo" })
  List<Post> findAllWithRelations();

  @Query("SELECT new com.cms.dto.DtoPostSummary(p.id, p.title, p.slug, p.status, p.orderIndex) FROM Post p")
  List<DtoPostSummary> findAllWithSummary();

  Optional<Post> findBySlug(String slug);
}
