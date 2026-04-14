package com.cms.controller.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.access.prepost.PreAuthorize;

import com.cms.controller.ICmsContentController;
import com.cms.dto.DtoCmsContent;
import com.cms.dto.DtoCmsContentIU;
import com.cms.dto.DtoCmsContentBulkRequest;
import com.cms.dto.PagedResponse;
import com.cms.entity.CmsContent;
import com.cms.entity.RootEntityResponse;
import com.cms.mapper.CmsContentMapper;
import com.cms.service.ICmsContentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/contents")
@RequiredArgsConstructor
public class CmsContentController extends BaseController implements ICmsContentController {

  private final ICmsContentService cmsContentService;
  private final CmsContentMapper cmsContentMapper;

  @Override
  @GetMapping("/section/list")
  @PreAuthorize("hasAuthority('contents:read')")
  public RootEntityResponse<List<String>> getSectionKeys() {
    List<String> sectionKeys = cmsContentService.getDistinctSectionKeys();
    return ok(sectionKeys);
  }

  @Override
  @GetMapping("/section/{sectionKey}")
  @PreAuthorize("hasAuthority('contents:read')")
  public RootEntityResponse<List<DtoCmsContent>> getContentsBySectionKey(@PathVariable String sectionKey) {
    List<CmsContent> contents = cmsContentService.getActiveCmsContentsBySectionKey(sectionKey);
    List<DtoCmsContent> dtoContents = cmsContentMapper.toDtoCmsContentList(contents);
    return ok(dtoContents);
  }

  @Override
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('contents:read')")
  public RootEntityResponse<DtoCmsContent> getContentById(@PathVariable UUID id) {
    CmsContent cmsContent = cmsContentService.getCmsContentById(id);
    DtoCmsContent dtoCmsContent = cmsContentMapper.toDtoCmsContent(cmsContent);
    return ok(dtoCmsContent);
  }

  @Override
  @GetMapping("/list")
  @PreAuthorize("hasAuthority('contents:read')")
  public RootEntityResponse<List<DtoCmsContent>> getAllContents() {
    List<CmsContent> contents = cmsContentService.getAllCmsContents();
    List<DtoCmsContent> dtoContents = cmsContentMapper.toDtoCmsContentList(contents);
    return ok(dtoContents);
  }

  @Override
  @GetMapping("/list/paged")
  @PreAuthorize("hasAuthority('contents:read')")
  public RootEntityResponse<PagedResponse<DtoCmsContent>> getAllContentsPaged(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "sortOrder,asc") String sort) {
    Pageable pageable = createPageable(page, size, sort);
    Page<CmsContent> pageResult = cmsContentService.getAllCmsContentsPaged(pageable);
    List<DtoCmsContent> dtoContents = cmsContentMapper.toDtoCmsContentList(pageResult.getContent());
    return ok(PagedResponse.from(pageResult, dtoContents));
  }

  @Override
  @PostMapping
  @PreAuthorize("hasAuthority('contents:create')")
  public RootEntityResponse<DtoCmsContent> createContent(@Valid @RequestBody DtoCmsContentIU dtoCmsContentIU) {
    CmsContent cmsContent = cmsContentMapper.toCmsContent(dtoCmsContentIU);

    com.cms.entity.CmsBasicInfo basicInfoPayload = null;
    if (dtoCmsContentIU.getBasicInfo() != null) {
      basicInfoPayload = new com.cms.mapper.CmsBasicInfoMapperImpl().toCmsBasicInfo(dtoCmsContentIU.getBasicInfo());
    }

    CmsContent savedContent = cmsContentService.createCmsContent(cmsContent, dtoCmsContentIU.getBasicInfoId(),
        basicInfoPayload);
    DtoCmsContent dtoCmsContent = cmsContentMapper.toDtoCmsContent(savedContent);
    return ok(dtoCmsContent);
  }

  @Override
  @PostMapping("/bulk")
  @PreAuthorize("hasAuthority('contents:create')")
  public RootEntityResponse<List<DtoCmsContent>> createBulkContents(
      @Valid @RequestBody DtoCmsContentBulkRequest request) {
    com.cms.entity.CmsBasicInfo basicInfoPayload = null;
    if (request.getBasicInfo() != null) {
      basicInfoPayload = new com.cms.mapper.CmsBasicInfoMapperImpl().toCmsBasicInfo(request.getBasicInfo());
    }

    List<CmsContent> contentsToSave = request.getContents().stream().map(item -> {
      CmsContent content = new CmsContent();
      content.setContentType(item.getContentType());
      content.setMetadata(item.getMetadata());
      return content;
    }).toList();

    List<CmsContent> savedContents = cmsContentService.createBulkCmsContents(request.getBasicInfoId(), basicInfoPayload,
        contentsToSave);
    List<DtoCmsContent> dtoContents = cmsContentMapper.toDtoCmsContentList(savedContents);
    return ok(dtoContents);
  }

  @Override
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('contents:update')")
  public RootEntityResponse<DtoCmsContent> updateContent(@PathVariable UUID id,
      @Valid @RequestBody DtoCmsContentIU dtoCmsContentIU) {
    CmsContent contentUpdate = cmsContentMapper.toCmsContent(dtoCmsContentIU);

    com.cms.entity.CmsBasicInfo basicInfoPayload = null;
    if (dtoCmsContentIU.getBasicInfo() != null) {
      basicInfoPayload = new com.cms.mapper.CmsBasicInfoMapperImpl().toCmsBasicInfo(dtoCmsContentIU.getBasicInfo());
    }

    CmsContent savedContent = cmsContentService.updateCmsContent(id, contentUpdate, dtoCmsContentIU.getBasicInfoId(),
        basicInfoPayload);
    DtoCmsContent dtoCmsContent = cmsContentMapper.toDtoCmsContent(savedContent);
    return ok(dtoCmsContent);
  }

  @Override
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('contents:delete')")
  public RootEntityResponse<Boolean> deleteContent(@PathVariable UUID id) {
    Boolean deleted = cmsContentService.deleteCmsContent(id);
    if (deleted) {
      return ok(deleted);
    }
    return error("Content not deleted");
  }
}
