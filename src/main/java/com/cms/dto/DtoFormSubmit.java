package com.cms.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for form submission request.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoFormSubmit {
  private Map<String, Object> payload;

  /**
   * Mail+Form v3 (B3) — Bot/spam tespiti icin gizli honeypot alani.
   * Gercek kullanicilarda bos kalmalidir; dolu gelirse 400 ile reddedilir.
   */
  private String honeypot;

  public DtoFormSubmit(Map<String, Object> payload) {
    this.payload = payload;
  }
}
