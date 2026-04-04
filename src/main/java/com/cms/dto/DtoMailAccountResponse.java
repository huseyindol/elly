package com.cms.dto;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * Mail hesabı yanıtı — smtpPassword hiçbir zaman dahil edilmez.
 */
@Getter
@Setter
public class DtoMailAccountResponse {

  private Long id;
  private String name;
  private String fromAddress;
  private String smtpHost;
  private Integer smtpPort;
  private String smtpUsername;
  private Boolean isDefault;
  private Boolean active;
  private Date createdAt;
  private Date updatedAt;
}
