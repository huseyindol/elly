package com.cms.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoEmailTemplate {

  private Long id;
  private String tenantId;
  private String templateKey;
  private String subject;
  private String htmlBody;
  private String description;
  private Boolean active;
  private Integer version;
  private Long optimisticLockVersion;
  private Date createdAt;
  private Date updatedAt;
}
