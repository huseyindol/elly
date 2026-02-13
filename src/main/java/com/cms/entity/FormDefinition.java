package com.cms.entity;

import org.hibernate.annotations.Type;

import com.cms.entity.form.FormSchema;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity for storing form definitions with JSON schema.
 * The schema field contains the complete form structure including fields,
 * validation rules, and conditions.
 */
@Entity
@Table(name = "form_definitions", indexes = {
    @Index(name = "idx_form_def_title", columnList = "title"),
    @Index(name = "idx_form_def_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FormDefinition extends BaseEntity {
  private String title;

  private Integer version;

  @Type(JsonType.class)
  @Column(columnDefinition = "jsonb")
  private FormSchema schema;

  private Boolean active;
}
