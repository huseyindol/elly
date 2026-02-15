package com.cms.entity.form;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Step definition for multi-step (wizard) forms.
 * Each step groups related fields together.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StepDefinition implements Serializable {
  private String id; // Unique step identifier
  private String title; // Step title
  private String description; // Step description
}
