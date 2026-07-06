package com.cms.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Toplu asset silme isteği (POST /api/v1/assets/bulk-delete). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoAssetsBulkDelete {

  @NotEmpty(message = "ids is required")
  private List<Long> ids;
}
