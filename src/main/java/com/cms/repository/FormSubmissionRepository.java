package com.cms.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.entity.FormSubmission;

public interface FormSubmissionRepository extends JpaRepository<FormSubmission, Long> {

  List<FormSubmission> findByFormDefinitionId(Long formDefinitionId);

  Page<FormSubmission> findByFormDefinitionId(Long formDefinitionId, Pageable pageable);

  @Query("SELECT fs FROM FormSubmission fs WHERE fs.formDefinition.id = :formId ORDER BY fs.submittedAt DESC")
  List<FormSubmission> findByFormIdOrderBySubmittedAtDesc(@Param("formId") Long formId);

  @Query("SELECT COUNT(fs) FROM FormSubmission fs WHERE fs.formDefinition.id = :formId")
  Long countByFormId(@Param("formId") Long formId);

  @Query("SELECT fs FROM FormSubmission fs WHERE fs.submittedAt BETWEEN :startDate AND :endDate")
  List<FormSubmission> findBySubmittedAtBetween(
      @Param("startDate") LocalDateTime startDate,
      @Param("endDate") LocalDateTime endDate);
}
