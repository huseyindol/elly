package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import com.cms.entity.FormDefinition;

public interface FormDefinitionRepository extends JpaRepository<FormDefinition, Long> {

  /**
   * {@code senderMailAccount} LAZY proxy oldugu icin {@code @Cacheable}
   * Redis'e serialize ederken null yazardi; sonraki cache hit'lerde submit
   * akisi senderMailAccount=null gorerek 500 atiyordu.
   * EntityGraph ile sender her getById'de initialize edilir, cache'e dolu yazilir.
   */
  @Override
  @EntityGraph(attributePaths = {"senderMailAccount"})
  @NonNull
  Optional<FormDefinition> findById(@NonNull Long id);

  List<FormDefinition> findByActiveTrue();

  Optional<FormDefinition> findByTitleAndVersion(String title, Integer version);

  @Query("SELECT f FROM FormDefinition f WHERE f.active = true ORDER BY f.title")
  List<FormDefinition> findAllActive();

  @Query("SELECT f FROM FormDefinition f WHERE f.active = true")
  Page<FormDefinition> findAllActivePaged(Pageable pageable);

  @Query("SELECT f FROM FormDefinition f WHERE LOWER(f.title) LIKE LOWER(CONCAT('%', :title, '%'))")
  List<FormDefinition> findByTitleContainingIgnoreCase(@Param("title") String title);
}
