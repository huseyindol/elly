package com.cms.service;

import com.cms.dto.DtoStorageQuota;

/**
 * Tenant depolama kotası — hepsi MEVCUT tenant context'inde (TenantContext) çalışır.
 * Admin bir tenant'ın kotasını ayarlamak için X-Tenant-Id ile o context'e girer.
 */
public interface IStorageQuotaService {

  /** Gelen {@code incomingBytes} mevcut tenant kotasını aşıyorsa QuotaExceededException (413). */
  void ensureWithin(long incomingBytes);

  /** Başarılı upload sonrası kullanımı artır (atomik). */
  void addUsage(long bytes);

  /** Dosya silindiğinde kullanımı azalt (atomik, 0'ın altına inmez). */
  void removeUsage(long bytes);

  /** Mevcut tenant'ın kullanım/limit durumu. */
  DtoStorageQuota currentUsage();

  /** Mevcut tenant'ın limitini ayarla (null → config varsayılanı). */
  DtoStorageQuota setLimit(Long limitBytes);

  /** Mevcut tenant klasörünü gezip used_bytes'ı gerçek boyutla düzeltir (drift onarımı). */
  DtoStorageQuota recompute();
}
