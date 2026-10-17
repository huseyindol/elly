package com.cms.service;

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

public interface IAiService {

  DtoAiSlugResponse generateSlug(DtoAiTitleRequest request);

  DtoAiSeoResponse generateSeoFields(DtoAiTitleRequest request);

  DtoAiDescriptionResponse generateDescription(DtoAiDescriptionRequest request);

  DtoAiAltTextResponse generateAltText(DtoAiTitleRequest request);

  DtoAiBlogInfoResponse generateBlogInfo(DtoAiBlogInfoRequest request);

  DtoAiArticleResponse generateArticle(DtoAiArticleRequest request);
}
