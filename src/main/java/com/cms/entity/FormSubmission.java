package com.cms.entity;

import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.Type;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity for storing form submissions.
 * The payload field contains the sanitized user responses as JSON.
 */
@Entity
@Table(name = "form_submissions", indexes = {
    @Index(name = "idx_form_sub_form_id", columnList = "form_definition_id"),
    @Index(name = "idx_form_sub_submitted_at", columnList = "submittedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FormSubmission extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "form_definition_id")
  private FormDefinition formDefinition;

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> payload;

  private LocalDateTime submittedAt;

  @PrePersist
  protected void onCreate() {
    this.submittedAt = LocalDateTime.now();
  }
}
