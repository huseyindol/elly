package com.cms.dto;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for FormSubmission responses.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoFormSubmission {
  private Long id;
  private Long formDefinitionId;
  private String formTitle;
  private Map<String, Object> payload;
  private LocalDateTime submittedAt;
  private Date createdAt;
}
