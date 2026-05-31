package com.cms.dto;

import com.cms.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoNotification {
  private Long id;
  private Long userId;
  private NotificationType type;
  private String title;
  private String message;
  private String link;
  private boolean read;
  private String tenantId;
  private Map<String, Object> metadata;
  private Date createdAt;
}
