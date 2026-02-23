package com.cms.controller;

import java.util.List;
import java.util.UUID;

import com.cms.dto.DtoCmsContent;
import com.cms.dto.DtoCmsContentIU;
import com.cms.dto.DtoCmsContentBulkRequest;
import com.cms.dto.PagedResponse;
import com.cms.entity.RootEntityResponse;

public interface ICmsContentController {

  RootEntityResponse<List<DtoCmsContent>> getContentsBySectionKey(String sectionKey);

  RootEntityResponse<DtoCmsContent> getContentById(UUID id);

  RootEntityResponse<List<DtoCmsContent>> getAllContents();

  RootEntityResponse<PagedResponse<DtoCmsContent>> getAllContentsPaged(int page, int size, String sort);

  RootEntityResponse<DtoCmsContent> createContent(DtoCmsContentIU dtoCmsContentIU);

  RootEntityResponse<List<DtoCmsContent>> createBulkContents(DtoCmsContentBulkRequest request);

  RootEntityResponse<DtoCmsContent> updateContent(UUID id, DtoCmsContentIU dtoCmsContentIU);

  RootEntityResponse<Boolean> deleteContent(UUID id);
}
