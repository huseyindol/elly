package com.cms.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cms.dto.DtoAiAltTextResponse;
import com.cms.dto.DtoAiArticleRequest;
import com.cms.dto.DtoAiArticleResponse;
import com.cms.dto.DtoAiBlogInfoRequest;
import com.cms.dto.DtoAiBlogInfoResponse;
import com.cms.dto.DtoAiDescriptionRequest;
import com.cms.dto.DtoAiDescriptionResponse;
import com.cms.dto.DtoAiQuota;
import com.cms.dto.DtoAiQuotaLimitRequest;
import com.cms.dto.DtoAiSeoResponse;
import com.cms.dto.DtoAiSlugResponse;
import com.cms.dto.DtoAiTitleRequest;
import com.cms.dto.DtoAiUsageLog;
import com.cms.dto.DtoAiUsageStats;

public interface IAiService {

  DtoAiSlugResponse generateSlug(DtoAiTitleRequest request);

  DtoAiSeoResponse generateSeoFields(DtoAiTitleRequest request);

  DtoAiDescriptionResponse generateDescription(DtoAiDescriptionRequest request);

  DtoAiAltTextResponse generateAltText(DtoAiTitleRequest request);

  DtoAiBlogInfoResponse generateBlogInfo(DtoAiBlogInfoRequest request);

  DtoAiArticleResponse generateArticle(DtoAiArticleRequest request);

  Page<DtoAiUsageLog> listUsageLogs(String action, String status, String username, String q, Pageable pageable);

  DtoAiUsageLog getUsageLogById(Long id);

  DtoAiUsageStats getUsageStats();

  DtoAiQuota getQuota();

  DtoAiQuota updateQuotaLimits(DtoAiQuotaLimitRequest request);
}
