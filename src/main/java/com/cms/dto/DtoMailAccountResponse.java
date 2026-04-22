package com.cms.dto;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * Mail hesabi yaniti — v2 DB-based.
 *
 * <p>SMTP password hicbir zaman response'a eklenmez. Aktiflik ve metadata
 * bilgileri Admin panelinde hesap secimi icin kullanilir.
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
  private Boolean active;
  private Date createdAt;
  private Date updatedAt;
}
