package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Panel-içi arama başlatma isteği: {@code /app/rtc/call}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoCallStart {
  /** Aranan panel kullanıcısının id'si (arayanla aynı tenant DB'sinde). */
  private Long calleeUserId;
}
