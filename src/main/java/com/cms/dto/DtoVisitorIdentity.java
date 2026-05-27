package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoVisitorIdentity {
  private Long id;
  private Long tenantUserId;
  private String displayName;
  private String email;
  private Date createdAt;
  private Date lastSeenAt;
}
