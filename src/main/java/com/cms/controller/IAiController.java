package com.cms.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cms.dto.DtoAiAltTextResponse;
import com.cms.dto.DtoAiArticleRequest;
import com.cms.dto.DtoAiArticleResponse;
import com.cms.dto.DtoAiBlogInfoRequest;
import com.cms.dto.DtoAiBlogInfoResponse;
import com.cms.dto.DtoAiDescriptionRequest;
import com.cms.dto.DtoAiDescriptionResponse;
import com.cms.dto.DtoAiSeoResponse;
import com.cms.dto.DtoAiSlugResponse;
import com.cms.dto.DtoAiTitleRequest;
import com.cms.dto.DtoAiUsageLog;
import com.cms.dto.DtoAiUsageStats;
import com.cms.entity.RootEntityResponse;

public interface IAiController {

  RootEntityResponse<DtoAiSlugResponse> generateSlug(DtoAiTitleRequest request);

  RootEntityResponse<DtoAiSeoResponse> generateSeoFields(DtoAiTitleRequest request);

  RootEntityResponse<DtoAiDescriptionResponse> generateDescription(DtoAiDescriptionRequest request);

  RootEntityResponse<DtoAiAltTextResponse> generateAltText(DtoAiTitleRequest request);

  RootEntityResponse<DtoAiBlogInfoResponse> generateBlogInfo(DtoAiBlogInfoRequest request);

  RootEntityResponse<DtoAiArticleResponse> generateArticle(DtoAiArticleRequest request);

  RootEntityResponse<Page<DtoAiUsageLog>> listUsageLogs(
      String action, String status, String username, String q, Pageable pageable);

  RootEntityResponse<DtoAiUsageLog> getUsageLogById(Long id);

  RootEntityResponse<DtoAiUsageStats> getUsageStats();
}
