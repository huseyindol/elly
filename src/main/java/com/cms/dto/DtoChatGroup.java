package com.cms.dto;

import com.cms.entity.ChatGroupType;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Data
public class DtoChatGroup {
  private UUID id;
  private String name;
  private String description;
  private ChatGroupType type;
  private Long createdBy;
  private int visibilityLevel;
  /** TC için tenant id; AC için null. */
  private String tenantId;
  /** TRUE ise website ziyaretçileri (Z) görebilir/yazabilir. */
  private boolean visitorAccess;
  private Date createdAt;
  private Date updatedAt;
}
