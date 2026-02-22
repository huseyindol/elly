package com.cms.dto;

import java.util.Date;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoCmsBasicInfo {
  private UUID id;
  private String sectionKey;
  private String title;
  private String description;
  private Boolean isActive;
  private Integer sortOrder;
  private Date createdAt;
  private Date updatedAt;
}
