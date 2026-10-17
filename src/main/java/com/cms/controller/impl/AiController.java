package com.cms.controller.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.controller.IAiController;
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
import com.cms.service.IAiService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController extends BaseController implements IAiController {

  private static final String CAN_EDIT_PAGES_OR_POSTS =
      "hasAnyAuthority('pages:create','pages:update','posts:create','posts:update')";
  private static final String CAN_EDIT_DESCRIPTION =
      "hasAnyAuthority('pages:create','pages:update','components:create','components:update','widgets:create','widgets:update')";
  private static final String CAN_EDIT_BANNERS =
      "hasAnyAuthority('banners:create','banners:update')";
  private static final String CAN_EDIT_POSTS =
      "hasAnyAuthority('posts:create','posts:update')";
  private static final String CAN_READ_AI_LOGS =
      "hasAnyAuthority('posts:read','pages:read','components:read','widgets:read','banners:read','users:manage')";

  private final IAiService aiService;

  @Override
  @PostMapping("/slug")
  @PreAuthorize(CAN_EDIT_PAGES_OR_POSTS)
  public RootEntityResponse<DtoAiSlugResponse> generateSlug(@Valid @RequestBody DtoAiTitleRequest request) {
    return ok(aiService.generateSlug(request));
  }

  @Override
  @PostMapping("/seo")
  @PreAuthorize(CAN_EDIT_PAGES_OR_POSTS)
  public RootEntityResponse<DtoAiSeoResponse> generateSeoFields(@Valid @RequestBody DtoAiTitleRequest request) {
    return ok(aiService.generateSeoFields(request));
  }

  @Override
  @PostMapping("/description")
  @PreAuthorize(CAN_EDIT_DESCRIPTION)
  public RootEntityResponse<DtoAiDescriptionResponse> generateDescription(
      @Valid @RequestBody DtoAiDescriptionRequest request) {
    return ok(aiService.generateDescription(request));
  }

  @Override
  @PostMapping("/alt-text")
  @PreAuthorize(CAN_EDIT_BANNERS)
  public RootEntityResponse<DtoAiAltTextResponse> generateAltText(@Valid @RequestBody DtoAiTitleRequest request) {
    return ok(aiService.generateAltText(request));
  }

  @Override
  @PostMapping("/blog-info")
  @PreAuthorize(CAN_EDIT_POSTS)
  public RootEntityResponse<DtoAiBlogInfoResponse> generateBlogInfo(
      @Valid @RequestBody DtoAiBlogInfoRequest request) {
    return ok(aiService.generateBlogInfo(request));
  }

  @Override
  @PostMapping("/article")
  @PreAuthorize(CAN_EDIT_POSTS)
  public RootEntityResponse<DtoAiArticleResponse> generateArticle(@Valid @RequestBody DtoAiArticleRequest request) {
    return ok(aiService.generateArticle(request));
  }

  @Override
  @GetMapping("/logs")
  @PreAuthorize(CAN_READ_AI_LOGS)
  public RootEntityResponse<Page<DtoAiUsageLog>> listUsageLogs(
      @RequestParam(value = "action", required = false) String action,
      @RequestParam(value = "status", required = false) String status,
      @RequestParam(value = "username", required = false) String username,
      @RequestParam(value = "q", required = false) String q,
      @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
    return ok(aiService.listUsageLogs(action, status, username, q, pageable));
  }

  @Override
  @GetMapping("/logs/{id}")
  @PreAuthorize(CAN_READ_AI_LOGS)
  public RootEntityResponse<DtoAiUsageLog> getUsageLogById(@PathVariable("id") Long id) {
    return ok(aiService.getUsageLogById(id));
  }

  @Override
  @GetMapping("/stats")
  @PreAuthorize(CAN_READ_AI_LOGS)
  public RootEntityResponse<DtoAiUsageStats> getUsageStats() {
    return ok(aiService.getUsageStats());
  }
}
