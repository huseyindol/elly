package com.cms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DtoChatGroupCreate {

  @NotBlank
  @Size(max = 100)
  private String name;

  @Size(max = 500)
  private String description;

  private List<Long> memberIds;

  /**
   * TC (Tenant Chat) için: hangi tenant'a ait olacağı. NULL ise klasik AC akışı (basedb'de tutulur).
   * Dolu ise group ilgili tenant DB'sine yazılır.
   */
  private String tenantId;

  /**
   * TRUE ise website ziyaretçileri (Z) bu group'u listeleyebilir ve yazabilir.
   * Sadece {@code tenantId} dolu iken anlamlı; AC group'ları visitor erişimine açılamaz.
   */
  private Boolean visitorAccess;
}
