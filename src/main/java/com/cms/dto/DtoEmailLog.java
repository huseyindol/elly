package com.cms.dto;

import java.util.Date;

import com.cms.enums.EmailStatus;

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
public class DtoEmailLog {
  private Long id;
  private String recipient;
  private String subject;
  private String templateName;
  private EmailStatus status;
  private int retryCount;
  private Date createdAt;
  private Date sentAt;
}
