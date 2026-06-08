package com.cms.controller;

import com.cms.dto.DtoStorageQuota;
import com.cms.dto.DtoStorageQuotaLimitRequest;
import com.cms.entity.RootEntityResponse;

/** Depolama kotası — mevcut tenant (X-Tenant-Id) bağlamında çalışır. */
public interface IStorageQuotaController {

  RootEntityResponse<DtoStorageQuota> getQuota();

  RootEntityResponse<DtoStorageQuota> setLimit(DtoStorageQuotaLimitRequest request);

  RootEntityResponse<DtoStorageQuota> recompute();
}
