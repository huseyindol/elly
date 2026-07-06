package com.cms.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Toplu asset silme sonucu. Silmeler bağımsızdır (biri başarısız olsa diğerleri
 * geri alınmaz); başarısız id'ler {@code failedIds}'te döner.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoAssetsBulkDeleteResult {
  private int deletedCount;
  private List<Long> failedIds;
}
